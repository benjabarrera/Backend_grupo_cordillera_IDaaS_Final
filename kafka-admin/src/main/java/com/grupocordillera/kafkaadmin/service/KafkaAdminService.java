package com.grupocordillera.kafkaadmin.service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KafkaAdminService {
    @Value("${spring.kafka.bootstrap-servers}") private String brokers;

    private AdminClient client() {
        return AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
            AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "8000",
            AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "12000"));
    }

    private void validateName(String name) {
        if (name == null || !name.matches("[a-zA-Z0-9._-]{1,249}") || name.startsWith("__"))
            throw new IllegalArgumentException("Nombre de topic invalido");
    }
    private void validateConfig(String key, String value) {
        if (!Set.of("retention.ms", "retention.bytes", "segment.ms", "cleanup.policy").contains(key))
            throw new IllegalArgumentException("Configuracion no permitida: " + key);
        if ("cleanup.policy".equals(key)) {
            if (!Set.of("delete", "compact").contains(value))
                throw new IllegalArgumentException("cleanup.policy debe ser delete o compact");
        } else {
            try {
                long n = Long.parseLong(value);
                if (n < 60000 && !"retention.bytes".equals(key))
                    throw new IllegalArgumentException("Valor demasiado pequeno: " + key);
                if ("retention.bytes".equals(key) && n < 1048576)
                    throw new IllegalArgumentException("retention.bytes debe ser al menos 1 MiB");
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Valor numerico invalido: " + key);
            }
        }
    }

    public List<String> topics() throws Exception {
        try (AdminClient admin = client()) {
            return admin.listTopics().names().get(12, TimeUnit.SECONDS).stream().sorted().toList();
        }
    }

    public Map<String,Object> info(String name) throws Exception {
        validateName(name);
        try (AdminClient admin = client()) {
            TopicDescription topic = admin.describeTopics(List.of(name))
                .allTopicNames().get(12, TimeUnit.SECONDS).get(name);
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
            Config configs = admin.describeConfigs(List.of(resource)).all().get(12, TimeUnit.SECONDS).get(resource);
            Map<String,String> relevant = new LinkedHashMap<>();
            for (String key: List.of("retention.ms","retention.bytes","cleanup.policy","segment.ms")) {
                ConfigEntry entry = configs.get(key);
                if (entry != null) relevant.put(key, entry.value());
            }
            List<Map<String,Object>> partitions = topic.partitions().stream().map(p -> {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("id",p.partition());
                row.put("replicas",p.replicas().stream().map(b->b.id()).toList());
                row.put("lider",p.leader() == null ? -1 : p.leader().id());
                return row;
            }).toList();
            return Map.of("topic",name,"particiones",partitions,"configuracion",relevant);
        }
    }

    public Map<String,Object> create(String name, int partitions, short replicas,
                                     Long retentionMs, String cleanup) throws Exception {
        validateName(name);
        if (!name.startsWith("cordillera-") && !name.startsWith("ep5-"))
            throw new IllegalArgumentException("Usa prefijo cordillera- o ep5- para el laboratorio");
        if (partitions < 1 || partitions > 24 || replicas < 1 || replicas > 2)
            throw new IllegalArgumentException("Particiones 1..24, replicas 1..2");
        Map<String,String> config = new HashMap<>();
        if (retentionMs != null) {validateConfig("retention.ms",retentionMs.toString()); config.put("retention.ms",retentionMs.toString());}
        if (cleanup != null) {validateConfig("cleanup.policy",cleanup); config.put("cleanup.policy",cleanup);}
        try (AdminClient admin = client()) {
            NewTopic topic = new NewTopic(name, partitions, replicas).configs(config);
            admin.createTopics(List.of(topic)).all().get(12, TimeUnit.SECONDS);
        }
        return info(name);
    }

    public Map<String,Object> updateConfig(String name, Map<String,String> config) throws Exception {
        validateName(name);
        if (config == null || config.isEmpty()) throw new IllegalArgumentException("Configuraciones vacias");
        for (var e:config.entrySet()) validateConfig(e.getKey(), e.getValue());
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        List<AlterConfigOp> operations = config.entrySet().stream()
            .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(),e.getValue()),AlterConfigOp.OpType.SET)).toList();
        try (AdminClient admin=client()) {
            admin.incrementalAlterConfigs(Map.of(resource, operations)).all().get(12,TimeUnit.SECONDS);
        }
        return info(name);
    }

    public Map<String,Object> increasePartitions(String name, int count) throws Exception {
        validateName(name);
        if (count < 1 || count > 24) throw new IllegalArgumentException("Particiones fuera del rango 1..24");
        try (AdminClient admin=client()) {
            admin.createPartitions(Map.of(name, NewPartitions.increaseTo(count))).all().get(12,TimeUnit.SECONDS);
        }
        return info(name);
    }

    public void delete(String name, boolean confirm) throws Exception {
        validateName(name);
        if (!confirm || !name.startsWith("ep5-"))
            throw new IllegalArgumentException("Solo se permite borrar topics temporales ep5-* con confirm=true");
        try (AdminClient admin=client()) {
            admin.deleteTopics(List.of(name)).all().get(12,TimeUnit.SECONDS);
        }
    }

    public List<String> groups() throws Exception {
        try (AdminClient admin=client()) {
            return admin.listConsumerGroups().all().get(12,TimeUnit.SECONDS).stream()
                .map(ConsumerGroupListing::groupId).sorted().toList();
        }
    }

    public List<Map<String,Object>> offsets(String groupId) throws Exception {
        if (groupId == null || !groupId.matches("[a-zA-Z0-9._-]{1,249}"))
            throw new IllegalArgumentException("Grupo no valido");
        try (AdminClient admin=client()) {
            Map<TopicPartition, OffsetAndMetadata> offsets = admin.listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata().get(12,TimeUnit.SECONDS);
            List<Map<String,Object>> result = new ArrayList<>();
            offsets.forEach((tp,metadata)-> result.add(Map.of("topic",tp.topic(),
                "particion",tp.partition(),"offsetConfirmado",metadata.offset())));
            result.sort(Comparator.comparing(r->String.valueOf(r.get("topic"))));
            return result;
        }
    }
}

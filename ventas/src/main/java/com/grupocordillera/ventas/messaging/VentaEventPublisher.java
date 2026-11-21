package com.grupocordillera.ventas.messaging;

import com.grupocordillera.ventas.dto.event.VentaRegistradaEvent;
import com.grupocordillera.ventas.model.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VentaEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(VentaEventPublisher.class);
    private static final String TOPIC = "cordillera-ventas";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public VentaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(Venta venta) {
        VentaRegistradaEvent evento = new VentaRegistradaEvent(
                UUID.randomUUID().toString(), venta.getId(), venta.getSkuProducto(),
                venta.getCantidad(), venta.getMontoTotal(), venta.getSucursal(),
                venta.getFecha() == null ? null : venta.getFecha().toString());
        kafkaTemplate.send(TOPIC, venta.getSkuProducto(), evento).whenComplete((resultado, error) -> {
            if (error != null) {
                log.error("Kafka: error al publicar ventaId={} topic={}", venta.getId(), TOPIC, error);
            } else {
                log.info("Kafka: venta publicada id={} particion={} offset={}",
                        venta.getId(), resultado.getRecordMetadata().partition(),
                        resultado.getRecordMetadata().offset());
            }
        });
    }
}

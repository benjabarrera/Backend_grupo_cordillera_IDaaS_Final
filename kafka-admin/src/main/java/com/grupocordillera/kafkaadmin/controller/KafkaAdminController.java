package com.grupocordillera.kafkaadmin.controller;

import com.grupocordillera.kafkaadmin.service.KafkaAdminService;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/kafka")
public class KafkaAdminController {
    private final KafkaAdminService service;
    public KafkaAdminController(KafkaAdminService service) { this.service = service; }

    public record NuevoTopic(String nombre, Integer particiones, Short replicacion,
                             Long retentionMs, String cleanupPolicy) {}
    public record NuevasParticiones(Integer total) {}

    @GetMapping("/topics") public Object topics() throws Exception { return service.topics(); }
    @GetMapping("/topics/{nombre}") public Object info(@PathVariable String nombre) throws Exception {return service.info(nombre);}
    @PostMapping("/topics") public Object create(@RequestBody NuevoTopic request) throws Exception {
        if (request == null || request.nombre()==null) throw new IllegalArgumentException("Falta nombre");
        return service.create(request.nombre(),request.particiones()==null?3:request.particiones(),
            request.replicacion()==null?(short)2:request.replicacion(),
            request.retentionMs(),request.cleanupPolicy());
    }
    @PatchMapping("/topics/{nombre}/config") public Object update(@PathVariable String nombre,
        @RequestBody Map<String,String> values) throws Exception {return service.updateConfig(nombre,values);}
    @PostMapping("/topics/{nombre}/particiones") public Object partitions(@PathVariable String nombre,
        @RequestBody NuevasParticiones values) throws Exception {
        if (values==null || values.total()==null) throw new IllegalArgumentException("Falta total");
        return service.increasePartitions(nombre,values.total());
    }
    @DeleteMapping("/topics/{nombre}") public ResponseEntity<Void> delete(@PathVariable String nombre,
        @RequestParam(defaultValue="false") boolean confirm) throws Exception {
        service.delete(nombre,confirm); return ResponseEntity.noContent().build();
    }
    @GetMapping("/groups") public Object groups() throws Exception {return service.groups();}
    @GetMapping("/groups/{grupo}/offsets") public Object offsets(@PathVariable String grupo) throws Exception {
        return service.offsets(grupo);
    }
}

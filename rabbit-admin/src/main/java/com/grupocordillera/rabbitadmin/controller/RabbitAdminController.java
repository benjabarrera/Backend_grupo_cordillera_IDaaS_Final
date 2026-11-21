package com.grupocordillera.rabbitadmin.controller;

import com.grupocordillera.rabbitadmin.service.RabbitAdminService;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/rabbit")
public class RabbitAdminController {
    private final RabbitAdminService service;
    public RabbitAdminController(RabbitAdminService service) { this.service=service; }
    public record BindingRequest(String exchange, String queue, String routingKey) {}

    @GetMapping(value="/queues",produces=MediaType.APPLICATION_JSON_VALUE)
    public String queues() {return service.queues();}
    @PutMapping("/queues/{name}") public Object queue(@PathVariable String name) {
        service.putQueue(name); return Map.of("creada",name);
    }
    @DeleteMapping("/queues/{name}") public ResponseEntity<Void> deleteQueue(@PathVariable String name) {
        service.deleteQueue(name); return ResponseEntity.noContent().build();
    }
    @GetMapping(value="/exchanges",produces=MediaType.APPLICATION_JSON_VALUE)
    public String exchanges() {return service.exchanges();}
    @PutMapping("/exchanges/{name}") public Object exchange(@PathVariable String name) {
        service.putExchange(name); return Map.of("creado",name);
    }
    @DeleteMapping("/exchanges/{name}") public ResponseEntity<Void> deleteExchange(@PathVariable String name) {
        service.deleteExchange(name); return ResponseEntity.noContent().build();
    }
    @GetMapping(value="/bindings",produces=MediaType.APPLICATION_JSON_VALUE)
    public String bindings() {return service.bindings();}
    @PostMapping("/bindings") public Object createBinding(@RequestBody BindingRequest request) {
        service.bind(request.exchange(),request.queue(),request.routingKey());
        return Map.of("exchange",request.exchange(),"queue",request.queue(),"routingKey",request.routingKey());
    }
    @DeleteMapping("/bindings") public Object deleteBinding(@RequestBody BindingRequest request) {
        return Map.of("eliminado",service.unbind(request.exchange(),request.queue(),request.routingKey()));
    }
}

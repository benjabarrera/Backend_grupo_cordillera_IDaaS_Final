package com.grupocordillera.rabbitadmin.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RabbitAdminService {
    private final RestClient client;
    private final String base;

    public RabbitAdminService(@Value("${rabbit.management.url}") String url,
                              @Value("${rabbit.management.username}") String user,
                              @Value("${rabbit.management.password}") String password) {
        this.base = url.replaceAll("/+$", "");
        this.client = RestClient.builder().defaultHeaders(h -> h.setBasicAuth(user,password)).build();
    }

    private static String encoded(String value) {
        return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+", "%20");
    }
    private URI uri(String path) {return URI.create(base+path);}
    private void validateName(String name) {
        if (name == null || !name.matches("[a-zA-Z0-9._-]{1,100}")
            || !(name.startsWith("cordillera.") || name.startsWith("ep5.")))
            throw new IllegalArgumentException("Nombre permitido: prefijo cordillera. o ep5.");
    }
    private static final String VHOST="%2F";

    public String queues() {
        return client.get().uri(uri("/api/queues/"+VHOST)).retrieve().body(String.class);
    }
    public String exchanges() {
        return client.get().uri(uri("/api/exchanges/"+VHOST)).retrieve().body(String.class);
    }
    public String bindings() {
        return client.get().uri(uri("/api/bindings/"+VHOST)).retrieve().body(String.class);
    }
    public void putQueue(String name) {
        validateName(name);
        client.put().uri(uri("/api/queues/"+VHOST+"/"+encoded(name)))
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("durable",true,"auto_delete",false,"arguments",Map.of()))
            .retrieve().toBodilessEntity();
    }
    public void deleteQueue(String name) {
        validateName(name);
        if (!name.startsWith("ep5.")) throw new IllegalArgumentException("Solo se borran recursos temporales ep5.*");
        client.delete().uri(uri("/api/queues/"+VHOST+"/"+encoded(name)))
            .retrieve().toBodilessEntity();
    }
    public void putExchange(String name) {
        validateName(name);
        client.put().uri(uri("/api/exchanges/"+VHOST+"/"+encoded(name)))
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("type","direct","durable",true,"auto_delete",false,"internal",false,"arguments",Map.of()))
            .retrieve().toBodilessEntity();
    }
    public void deleteExchange(String name) {
        validateName(name);
        if (!name.startsWith("ep5.")) throw new IllegalArgumentException("Solo se borran recursos temporales ep5.*");
        client.delete().uri(uri("/api/exchanges/"+VHOST+"/"+encoded(name)))
            .retrieve().toBodilessEntity();
    }
    public void bind(String exchange, String queue, String routingKey) {
        validateName(exchange); validateName(queue);
        if (routingKey == null || routingKey.isBlank()) throw new IllegalArgumentException("Falta routingKey");
        client.post().uri(uri("/api/bindings/"+VHOST+"/e/"+encoded(exchange)+"/q/"+encoded(queue)))
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("routing_key",routingKey,"arguments",Map.of()))
            .retrieve().toBodilessEntity();
    }
    @SuppressWarnings("unchecked")
    public boolean unbind(String exchange, String queue, String routingKey) {
        validateName(exchange); validateName(queue);
        if (!exchange.startsWith("ep5.") || !queue.startsWith("ep5."))
            throw new IllegalArgumentException("Desvincular solo recursos ep5.*");
        String path="/api/bindings/"+VHOST+"/e/"+encoded(exchange)+"/q/"+encoded(queue);
        List<?> rows=client.get().uri(uri(path)).retrieve().body(List.class);
        if (rows == null) return false;
        for (Object item:rows) {
            if (item instanceof Map<?,?> map && routingKey.equals(map.get("routing_key"))) {
                Object props=map.get("properties_key");
                if (props!=null) {
                    client.delete().uri(uri(path+"/"+encoded(props.toString())))
                        .retrieve().toBodilessEntity();
                    return true;
                }
            }
        }
        return false;
    }
}

package com.grupocordillera.inventario.messaging;

import com.grupocordillera.inventario.dto.event.StockActualizadoEvent;
import com.grupocordillera.inventario.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class StockEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(StockEventPublisher.class);
    private static final String TOPIC = "cordillera-inventario";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public StockEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(Producto producto) {
        StockActualizadoEvent evento = new StockActualizadoEvent(
                UUID.randomUUID().toString(), producto.getSku(), producto.getStock(),
                producto.getStockMinimo(), producto.getEstado().name(),
                producto.getSucursal(), Instant.now().toString());
        kafkaTemplate.send(TOPIC, producto.getSku(), evento).whenComplete((resultado, error) -> {
            if (error != null) {
                log.error("Kafka: no se pudo publicar stock de sku={}", producto.getSku(), error);
            } else {
                log.info("Kafka: stock publicado sku={} particion={} offset={}",
                        producto.getSku(), resultado.getRecordMetadata().partition(),
                        resultado.getRecordMetadata().offset());
            }
        });
    }
}

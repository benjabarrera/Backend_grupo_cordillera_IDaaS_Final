package com.grupocordillera.alertas.messaging;

import com.grupocordillera.alertas.dto.event.VentaRegistradaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class VentaEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(VentaEventConsumer.class);
    private final JsonMapper jsonMapper;

    public VentaEventConsumer(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "cordillera-ventas", groupId = "cordillera-alertas-ventas")
    public void procesar(String json) {
        VentaRegistradaEvent evento = jsonMapper.readValue(json, VentaRegistradaEvent.class);
        if (evento.eventId() == null || evento.ventaId() == null || evento.montoTotal() == null
                || evento.ventaId() <= 0 || evento.montoTotal() < 0) {
            throw new IllegalArgumentException("Evento de venta invalido");
        }
        // Solo registrar y observar: NO repetir la venta ni descontar stock.
        log.info("Kafka RECIBIDO ventas | eventId={} ventaId={} sku={} monto={}",
                evento.eventId(), evento.ventaId(), evento.skuProducto(), evento.montoTotal());
    }
}

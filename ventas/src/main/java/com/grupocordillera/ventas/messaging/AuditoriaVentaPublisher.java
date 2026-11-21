package com.grupocordillera.ventas.messaging;

import com.grupocordillera.ventas.config.RabbitAuditoriaTopology;
import com.grupocordillera.ventas.dto.event.AuditoriaVentaEvent;
import com.grupocordillera.ventas.model.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;

@Service
public class AuditoriaVentaPublisher {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaVentaPublisher.class);
    private final RabbitTemplate template;
    private final JsonMapper mapper;

    public AuditoriaVentaPublisher(RabbitTemplate template, JsonMapper mapper) {
        this.template = template;
        this.mapper = mapper;
    }

    public void publicar(Venta venta) {
        var evento = new AuditoriaVentaEvent(
            UUID.randomUUID().toString(), "VENTA_REGISTRADA", venta.getId(),
            venta.getSkuProducto(), venta.getMontoTotal(), venta.getSucursal(),
            venta.getFecha() == null ? null : venta.getFecha().toString());
        try {
            String json = mapper.writeValueAsString(evento);
            template.convertAndSend(RabbitAuditoriaTopology.MAIN_EXCHANGE,
                RabbitAuditoriaTopology.MAIN_ROUTING, json);
            log.info("RabbitMQ auditoria enviada ventaId={} eventId={}", venta.getId(), evento.eventId());
        } catch (Exception ex) {
            // No revertir una venta YA guardada por un fallo de infraestructura.
            // Esto es best-effort: una entrega garantizada requiere outbox transaccional.
            log.error("RabbitMQ no disponible: auditoria ventaId={} no confirmada", venta.getId(), ex);
        }
    }
}

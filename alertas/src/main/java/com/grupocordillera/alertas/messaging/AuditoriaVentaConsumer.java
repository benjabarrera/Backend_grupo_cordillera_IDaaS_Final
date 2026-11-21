package com.grupocordillera.alertas.messaging;

import com.grupocordillera.alertas.config.RabbitAuditoriaTopology;
import com.grupocordillera.alertas.dto.event.AuditoriaVentaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AuditoriaVentaConsumer {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaVentaConsumer.class);
    private final JsonMapper mapper;

    public AuditoriaVentaConsumer(JsonMapper mapper) { this.mapper = mapper; }

    @RabbitListener(queues = RabbitAuditoriaTopology.MAIN_QUEUE,
                    containerFactory = "auditoriaListenerFactory")
    public void consumir(String mensaje) {
        try {
            AuditoriaVentaEvent event = mapper.readValue(mensaje, AuditoriaVentaEvent.class);
            if (event == null || event.ventaId() == null || event.ventaId() <= 0
                    || event.eventId() == null || event.eventId().isBlank()
                    || !"VENTA_REGISTRADA".equals(event.tipo())
                    || event.montoTotal() == null || event.montoTotal() < 0) {
                throw new IllegalArgumentException("Campos de auditoria invalidos");
            }
            // Auditoria solo registra: NO crear otra venta, NO descontar stock.
            log.info("RabbitMQ AUDITORIA RECIBIDA eventId={} ventaId={} monto={}",
                event.eventId(), event.ventaId(), event.montoTotal());
        } catch (Exception exception) {
            log.error("RabbitMQ auditoria invalida; se aplicaran reintentos y DLQ: {}",
                exception.getMessage());
            throw new IllegalArgumentException("Error procesando evento de auditoria", exception);
        }
    }
}

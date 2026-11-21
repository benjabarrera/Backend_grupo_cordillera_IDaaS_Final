package com.grupocordillera.alertas.messaging;

import com.grupocordillera.alertas.dto.CrearAlertaRequest;
import com.grupocordillera.alertas.dto.event.StockActualizadoEvent;
import com.grupocordillera.alertas.model.Alerta;
import com.grupocordillera.alertas.service.AlertaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class StockEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(StockEventConsumer.class);
    private final JsonMapper jsonMapper;
    private final AlertaService alertaService;

    public StockEventConsumer(JsonMapper jsonMapper, AlertaService alertaService) {
        this.jsonMapper = jsonMapper;
        this.alertaService = alertaService;
    }

    @KafkaListener(topics = "cordillera-inventario", groupId = "cordillera-alertas-inventario")
    public void procesar(String json) {
        StockActualizadoEvent evento = jsonMapper.readValue(json, StockActualizadoEvent.class);
        if (evento.eventId() == null || evento.sku() == null || evento.sku().isBlank()
                || evento.stock() == null || evento.stockMinimo() == null
                || evento.stock() < 0 || evento.stockMinimo() < 0) {
            throw new IllegalArgumentException("Evento de inventario invalido");
        }
        log.info("Kafka RECIBIDO inventario | eventId={} sku={} stock={}",
                evento.eventId(), evento.sku(), evento.stock());
        if (evento.stock() <= evento.stockMinimo()) {
            CrearAlertaRequest alerta = new CrearAlertaRequest();
            alerta.setCodigo("STOCK_CRITICO:" + evento.sku());
            alerta.setTipo(Alerta.TipoAlerta.STOCK_CRITICO);
            alerta.setTitulo("Stock critico");
            alerta.setDetalle("SKU " + evento.sku() + " tiene stock " + evento.stock());
            // AlertaService usa el codigo como clave de actualizacion en vez de duplicar filas.
            alertaService.crearAlerta(alerta);
        }
    }
}

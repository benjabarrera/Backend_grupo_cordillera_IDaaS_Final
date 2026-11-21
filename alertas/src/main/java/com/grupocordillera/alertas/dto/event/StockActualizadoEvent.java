package com.grupocordillera.alertas.dto.event;

public record StockActualizadoEvent(
        String eventId, String sku, Integer stock, Integer stockMinimo,
        String estado, String sucursal, String timestamp) {
}

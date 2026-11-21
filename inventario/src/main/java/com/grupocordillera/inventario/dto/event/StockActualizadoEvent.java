package com.grupocordillera.inventario.dto.event;

public record StockActualizadoEvent(
        String eventId,
        String sku,
        Integer stock,
        Integer stockMinimo,
        String estado,
        String sucursal,
        String timestamp) {
}

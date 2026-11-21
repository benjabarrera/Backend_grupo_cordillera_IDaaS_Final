package com.grupocordillera.alertas.dto.event;

public record VentaRegistradaEvent(
        String eventId, Long ventaId, String skuProducto, Integer cantidad,
        Double montoTotal, String sucursal, String fecha) {
}

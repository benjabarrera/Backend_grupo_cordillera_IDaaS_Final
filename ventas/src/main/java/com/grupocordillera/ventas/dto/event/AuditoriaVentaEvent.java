package com.grupocordillera.ventas.dto.event;

public record AuditoriaVentaEvent(
    String eventId,
    String tipo,
    Long ventaId,
    String skuProducto,
    Double montoTotal,
    String sucursal,
    String fecha
) {}

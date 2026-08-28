package com.primeraPulpa.dto;

public record DetalleEstadisticaMesDTO(
        String nombreMix,
        double cantidadVendida,
        double facturado,
        double costo,
        double ganancia
) {
}

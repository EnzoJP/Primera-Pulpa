package com.primeraPulpa.dto;

public record EstadisticaMensualDTO(
        int anio,
        int mes,
        String nombreMes,
        double kgVendidos,
        double facturado,
        double costoProduccion,
        double rentabilidad,
        double margenPorcentaje,
        double kgIngresados,
        double kgElaborados
) {
}

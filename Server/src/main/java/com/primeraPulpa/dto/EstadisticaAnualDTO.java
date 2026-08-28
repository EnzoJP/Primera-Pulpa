package com.primeraPulpa.dto;

public record EstadisticaAnualDTO(
        int anio,
        double kgVendidos,
        double facturado,
        double costoProduccion,
        double rentabilidad,
        double margenPorcentaje,
        double kgIngresados,
        double kgElaborados
) {
}

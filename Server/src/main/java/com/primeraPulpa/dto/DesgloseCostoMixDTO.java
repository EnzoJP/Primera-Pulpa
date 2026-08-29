package com.primeraPulpa.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DesgloseCostoMixDTO {

    // Presentación del mix (kg por unidad)
    private double cantidadPorUnidad;

    // Costo de materia prima
    private FormulaDesglose formula;

    // Costos adicionales por paquete que aplican a esta presentación
    private List<AdicionalDesglose> adicionales = new ArrayList<>();

    private double costoMateriaPrimaPorKg;
    private double costosAdicionalesPorKg;
    private double costoFinalPorKg;

    public double getCostoFinalPorUnidad() {
        return (costoMateriaPrimaPorKg + costosAdicionalesPorKg) * cantidadPorUnidad;
    }

    public double getTotalAdicionalPorPaquete() {
        return adicionales.stream().mapToDouble(a -> a.getValorPorPaquete()).sum();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FormulaDesglose {
        // Cantidad total que produce la fórmula (kg)
        private double cantidad;
        private List<DetalleDesglose> detalles = new ArrayList<>();
        private double subtotalMateriaPrima;
        private double costoPorKg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DetalleDesglose {
        private String materiaPrima;
        private double gramos;
        private double precioPorKg;
        private double costo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AdicionalDesglose {
        private String descripcion;
        private String presentacion;
        private double valorPorPaquete;
        private double aportePorKg;
    }
}

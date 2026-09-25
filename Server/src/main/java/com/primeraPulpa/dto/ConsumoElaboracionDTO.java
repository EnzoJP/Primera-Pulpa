package com.primeraPulpa.dto;

import java.time.LocalDate;

/**
 * Resumen de un desglose de consumo real de un LoteMix: qué materia prima se
 * usó, de qué lote provino y cuántos kg. Se muestra en los históricos de
 * StockMix (y como referencia de lote en StockMP).
 */
public class ConsumoElaboracionDTO {

    private final String materiaPrima;
    private final String numeroLote;
    private final LocalDate vencimiento;
    private final double cantidad;

    public ConsumoElaboracionDTO(String materiaPrima, String numeroLote,
                                 LocalDate vencimiento, double cantidad) {
        this.materiaPrima = materiaPrima;
        this.numeroLote = numeroLote;
        this.vencimiento = vencimiento;
        this.cantidad = cantidad;
    }

    public String getMateriaPrima() { return materiaPrima; }
    public String getNumeroLote() { return numeroLote; }
    public LocalDate getVencimiento() { return vencimiento; }
    public double getCantidad() { return cantidad; }
}
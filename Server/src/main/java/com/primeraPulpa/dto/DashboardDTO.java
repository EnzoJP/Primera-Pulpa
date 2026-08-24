package com.primeraPulpa.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDTO {
    private BigDecimal valorTotalInventario;
    private long totalMateriasPrimas;
    private long totalMixes;
    private long totalAlertasStockBajo;

    private List<ItemStockBajoDTO> alertasStockBajo;
    private List<UltimoIngresoDTO> ultimosIngresos;
    // Dentro de DashboardDTO.java
    private List<String> chartLabels;
    private List<Double> chartData;

    public List<String> getChartLabels() { return chartLabels; }
    public void setChartLabels(List<String> chartLabels) { this.chartLabels = chartLabels; }

    public List<Double> getChartData() { return chartData; }
    public void setChartData(List<Double> chartData) { this.chartData = chartData; }
    // Getters y Setters
    public BigDecimal getValorTotalInventario() { return valorTotalInventario; }
    public void setValorTotalInventario(BigDecimal valorTotalInventario) { this.valorTotalInventario = valorTotalInventario; }

    public long getTotalMateriasPrimas() { return totalMateriasPrimas; }
    public void setTotalMateriasPrimas(long totalMateriasPrimas) { this.totalMateriasPrimas = totalMateriasPrimas; }

    public long getTotalMixes() { return totalMixes; }
    public void setTotalMixes(long totalMixes) { this.totalMixes = totalMixes; }

    public long getTotalAlertasStockBajo() { return totalAlertasStockBajo; }
    public void setTotalAlertasStockBajo(long totalAlertasStockBajo) { this.totalAlertasStockBajo = totalAlertasStockBajo; }

    public List<ItemStockBajoDTO> getAlertasStockBajo() { return alertasStockBajo; }
    public void setAlertasStockBajo(List<ItemStockBajoDTO> alertasStockBajo) { this.alertasStockBajo = alertasStockBajo; }

    public List<UltimoIngresoDTO> getUltimosIngresos() { return ultimosIngresos; }
    public void setUltimosIngresos(List<UltimoIngresoDTO> ultimosIngresos) { this.ultimosIngresos = ultimosIngresos; }

    // Subclases DTO para listas
    public static class ItemStockBajoDTO {
        private Long id;
        private String nombre;
        private Double stockActual;
        private Double stockMinimo;
        private String unidad;

        public ItemStockBajoDTO(Long id, String nombre, Double stockActual, Double stockMinimo, String unidad) {
            this.id = id;
            this.nombre = nombre;
            this.stockActual = stockActual;
            this.stockMinimo = stockMinimo;
            this.unidad = unidad;
        }
        // Getters
        public Long getId() { return id; }
        public String getNombre() { return nombre; }
        public Double getStockActual() { return stockActual; }
        public Double getStockMinimo() { return stockMinimo; }
        public String getUnidad() { return unidad; }
    }

    public static class UltimoIngresoDTO {
        private Long id;
        private String nombre;
        private Double cantidad;
        private String unidad;
        private String fecha;

        public UltimoIngresoDTO(Long id, String nombre, Double cantidad, String unidad, String fecha) {
            this.id = id;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.unidad = unidad;
            this.fecha = fecha;
        }
        // Getters
        public Long getId() { return id; }
        public String getNombre() { return nombre; }
        public Double getCantidad() { return cantidad; }
        public String getUnidad() { return unidad; }
        public String getFecha() { return fecha; }
    }
}
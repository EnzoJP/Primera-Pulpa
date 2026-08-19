package com.primeraPulpa.dto;

import com.primeraPulpa.entities.LoteMix;

import java.time.LocalDate;
import java.util.List;

public class LoteDiarioDTO {

    private final LocalDate fecha;
    private final List<LoteMix> items;

    public LoteDiarioDTO(LocalDate fecha, List<LoteMix> items) {
        this.fecha = fecha;
        this.items = items;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public List<LoteMix> getItems() {
        return items;
    }

    public int getCantidadMixes() {
        return items.size();
    }

    public double getTotalKg() {
        return items.stream()
                .mapToDouble(l -> l.getCantidadElaborada() != null ? l.getCantidadElaborada() : 0.0)
                .sum();
    }
}
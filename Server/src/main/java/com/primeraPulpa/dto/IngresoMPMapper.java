package com.primeraPulpa.dto;

import com.primeraPulpa.entities.IngresoMP;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IngresoMPMapper {

    /**
     * Mapea un IngresoMP a su resumen para el listado.
     * Calcula cantidadItems y totalCosto en el servidor para no exponer la colección completa a la vista.
     */
    public IngresoResumenDTO toResumen(IngresoMP ingreso) {
        int cantidadItems = ingreso.getDetalles() != null ? ingreso.getDetalles().size() : 0;


        return new IngresoResumenDTO(
                ingreso.getId(),
                ingreso.getFechaHora(),
                cantidadItems
        );
    }

    public List<IngresoResumenDTO> toResumenList(List<IngresoMP> ingresos) {
        return ingresos.stream()
                .map(this::toResumen)
                .toList();
    }
}

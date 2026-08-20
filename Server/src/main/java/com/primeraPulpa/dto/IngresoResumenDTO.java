package com.primeraPulpa.dto;

import java.time.LocalDateTime;

/**
 * Evita cargar la colección completa de detalles por cada fila de la tabla.
 */
public record IngresoResumenDTO(
                Long id,
                LocalDateTime fechaHora,
                int cantidadItems,
                double cantidadTotal
                ) {
}

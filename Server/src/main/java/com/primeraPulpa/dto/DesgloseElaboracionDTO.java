package com.primeraPulpa.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Desglose propuesto (FEFO) de los lotes a consumir para una elaboración.
 * Se usa tanto en el endpoint JSON del formulario (para precargar de forma
 * ajustable el reparto) como en la validación del guardado.
 */
public record DesgloseElaboracionDTO(
        Long mixId,
        String mixNombre,
        double cantidadElaborada,
        List<ItemMateriaPrima> items
) {

    public record ItemMateriaPrima(
            Long materiaPrimaId,
            String materiaPrimaNombre,
            String unidad,
            double necesario,
            List<LoteSugerido> lotes
    ) {
    }

    public record LoteSugerido(
            Long loteId,
            String numeroLote,
            LocalDate vencimiento,
            double restante,
            double sugerido
    ) {
    }
}
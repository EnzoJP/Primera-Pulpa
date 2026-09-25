package com.primeraPulpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Trazabilidad del desglose de consumo de lotes de materia prima en una
 * elaboración: qué lote (DetalleIngresoMP) se usó y cuántos kg se consumieron
 * al producir un LoteMix. Permite el modo híbrido: el sistema propone un reparto
 * FEFO y el operario puede ajustarlo antes de confirmar.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetalleConsumoLote extends BaseEntity<Long> {

    @ManyToOne(optional = false)
    private LoteMix loteMix;

    @ManyToOne(optional = false)
    private MateriaPrima materiaPrima;

    @ManyToOne(optional = false)
    private DetalleIngresoMP lote;

    // Kilogramos de esta materia prima aportados por ese lote a la elaboración.
    private double cantidadConsumida;

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Boolean getEliminado() {
        return this.eliminado;
    }

    @Override
    public void setEliminado(Boolean eliminado) {
        this.eliminado = eliminado;
    }
}
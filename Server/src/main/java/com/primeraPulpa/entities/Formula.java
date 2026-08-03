package com.primeraPulpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Formula extends BaseEntity<Long> {

    // NOTA: no está en el diagrama original, agregado porque sin cantidad
    // la fórmula no puede calcular el consumo (ver HU-07). Confirmar con el equipo.
    private double cantidad;

    @ManyToOne
    private Mix mix;

    @ManyToOne
    private MateriaPrima materiaPrima;

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

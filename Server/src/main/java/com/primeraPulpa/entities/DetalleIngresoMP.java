package com.primeraPulpa.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetalleIngresoMP extends BaseEntity<Long> {

    @ManyToOne(optional = false)
    private IngresoMP ingresoMP;

    @ManyToOne(optional = false)
    private MateriaPrima materiaPrima;

    private double cantidad;

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

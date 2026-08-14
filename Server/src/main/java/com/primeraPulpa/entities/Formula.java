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

    // Porcentaje que representa esta materia prima dentro del mix.
    // No depende de la cantidad total a producir: si se producen 25 kg,
    // la cantidad de cada materia prima es 25 * (porcentaje / 100).
    private double porcentaje;

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

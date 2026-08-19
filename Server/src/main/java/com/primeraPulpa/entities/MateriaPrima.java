package com.primeraPulpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MateriaPrima extends BaseEntity<Long> {

    private String nombre;

    @ManyToOne
    private UnidadMedida unidadMedida;
    private double precio;
    private double cantidadActual;
    private double cantidadMinima;
    private LocalDate fechaIngreso;

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

    public void actualizarStock(double cantidad) {
        this.cantidadActual += cantidad;
    }
}

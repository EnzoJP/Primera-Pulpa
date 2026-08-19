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
public class Mix extends BaseEntity<Long> {

    private String nombre;

    // Puede no estar definido todavía (se define viendo los costos del mix)
    private Double precioVenta;

    // Costo por kg del mix. Se calcula desde la fórmula (gramos de cada
    // materia prima * precio, dividido la cantidad que produce) más los
    // costos adicionales del catálogo (bolsa, etiqueta, etc.).
    private double costo;

    //stock actual del mix. Se actualiza cada vez que se registra una elaboración o venta.
    private double stock;

    // Usuario que realizó la elaboración de este mix. INNCESARIO
    //@ManyToOne
    //private Usuario usuario;

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
        this.stock += cantidad;
    }
}
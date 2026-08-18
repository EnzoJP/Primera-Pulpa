package com.primeraPulpa.entities;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CostoAdicional extends BaseEntity<Long> {

    // Costo adicional al precio por unidad del mix (ej: bolsa, etiqueta).
    // Es un catálogo compartido por todos los mixes, no se relaciona con Mix.
    private String descripcion;
    private double valor;

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
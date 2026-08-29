package com.primeraPulpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    // Presentación a la que aplica este costo (1 kg, 5 kg o todos).
    // El valor se interpreta como $ por unidad de esa presentación.
    @Enumerated(EnumType.STRING)
    private PresentacionCosto presentacion;

    // Presentación por defecto: "Todos".
    public PresentacionCosto getPresentacionOrDefault() {
        return presentacion != null ? presentacion : PresentacionCosto.TODOS;
    }

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
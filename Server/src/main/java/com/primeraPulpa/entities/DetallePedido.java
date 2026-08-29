package com.primeraPulpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetallePedido extends BaseEntity<Long> {

    private double cantidad;
    private double precioUnitario;

    @ManyToOne
    private Pedido pedido;

    @ManyToOne
    private Mix mix;

    // Indica si este mix específico ya fue elaborado/preparado y descontado del stock de Mix
    @Builder.Default
    private Boolean preparado = false;

    public Boolean getPreparado() {
        return this.preparado != null ? this.preparado : false;
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

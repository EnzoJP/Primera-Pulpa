package com.primeraPulpa.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IngresoMP extends BaseEntity<Long> {

    private LocalDateTime fechaHora;

    @OneToMany(mappedBy = "ingresoMP", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleIngresoMP> detalles = new ArrayList<>();

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

    public double getCantidadIngresda(){
        return detalles.stream().mapToDouble(DetalleIngresoMP::getCantidad).sum();
    }
}

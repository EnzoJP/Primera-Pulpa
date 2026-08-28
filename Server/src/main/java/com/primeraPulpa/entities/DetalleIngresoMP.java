package com.primeraPulpa.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    // Costo unitario de la materia prima al momento de la recepción del lote.
    private double costoUnitario;

    // Stock restante de este lote. Null en lotes creados antes del FIFO: se interpreta como la cantidad completa.
    private Double cantidadRestante;

    // Vencimiento del lote/cosecha recibida. Null si no aplica (ej. insumos sin vencimiento).
    private LocalDate fechaVencimiento;

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

    public double getRestante() {
        return cantidadRestante != null ? cantidadRestante : cantidad;
    }
}

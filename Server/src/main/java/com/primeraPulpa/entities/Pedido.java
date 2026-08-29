package com.primeraPulpa.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pedido extends BaseEntity<Long> {

    private LocalDate fecha;

    @ManyToOne
    private Cliente cliente;

    @ManyToOne
    private EstadoPedido estadoPedido;

    // Usuario que registró el pedido
    @ManyToOne
    private Usuario usuario;

    // Composición: los detalles pertenecen exclusivamente al pedido
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

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

    public long getCantidadDetallesPreparados() {
        if (detalles == null || detalles.isEmpty()) return 0;
        return detalles.stream()
                .filter(d -> !Boolean.TRUE.equals(d.getEliminado()))
                .filter(d -> Boolean.TRUE.equals(d.getPreparado()))
                .count();
    }

    public long getTotalDetalles() {
        if (detalles == null || detalles.isEmpty()) return 0;
        return detalles.stream()
                .filter(d -> !Boolean.TRUE.equals(d.getEliminado()))
                .count();
    }

    public int getPorcentajePreparado() {
        long total = getTotalDetalles();
        if (total == 0) return 0;
        return (int) Math.round(((double) getCantidadDetallesPreparados() / total) * 100.0);
    }

    public boolean isCompletamentePreparado() {
        long total = getTotalDetalles();
        return total > 0 && getCantidadDetallesPreparados() == total;
    }
}

package com.primeraPulpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
public class LoteMix extends BaseEntity<Long> {

    @ManyToOne
    private Mix mix;

    // Usuario que registró la elaboración
    @ManyToOne
    private Usuario usuario;

    private LocalDate fechaElaboracion;
    //private Double cantidadInicial; No se si nos interesa
    private Double cantidadElaborada;

    // Desglose de lotes de materia prima consumidos (se carga bajo demanda en la vista).
    @Transient
    private List<DetalleConsumoLote> consumos = new ArrayList<>();

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

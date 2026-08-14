package com.primeraPulpa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MateriaPrimaDTO extends BaseDTO {

    private String nombre;
    private Long unidadMedidaId;
    private double precio;
    private double cantidadActual;
    private double cantidadMinima;
    private LocalDate fechaIngreso;

}

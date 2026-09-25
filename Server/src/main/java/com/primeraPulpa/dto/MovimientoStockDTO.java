package com.primeraPulpa.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Representa un movimiento que afectó el stock de una materia prima o de un mix.
 * Se usa tanto para el historial de StockMP (IngresoMP / ElaboracionMix) como
 * para el de StockMix (ElaboracionMix / Pedido).
 */
public class MovimientoStockDTO {

    /** Tipo de movimiento: IngresoMP, ElaboracionMix o Pedido. */
    private final String tipo;
    private final LocalDate fecha;
    /** Cantidad con signo: positiva (entrada) o negativa (salida). */
    private final double cantidad;
    /** Descripción del documento/movimiento (ej. nombre del mix o materia prima, nro de pedido). */
    private final String descripcion;
    /** Usuario responsable del movimiento. */
    private final String usuario;
    /** Id del documento origen (IngresoMP, LoteMix o Pedido). */
    private final Long documentoId;
    /** Saldo acumulado de la entidad luego de aplicar este movimiento. */
    private final double saldo;
    /** Número(s) de lote involucrado(s) en el movimiento (IngresoMP o consumo de ElaboracionMix). */
    private final String lote;
    /** Desglose real por lote para movimientos de ElaboracionMix (null si no aplica). */
    private final List<ConsumoElaboracionDTO> consumos;

    public MovimientoStockDTO(String tipo, LocalDate fecha, double cantidad, String descripcion,
                              String usuario, Long documentoId, double saldo) {
        this(tipo, fecha, cantidad, descripcion, usuario, documentoId, saldo, null, null);
    }

    public MovimientoStockDTO(String tipo, LocalDate fecha, double cantidad, String descripcion,
                              String usuario, Long documentoId, double saldo, String lote,
                              List<ConsumoElaboracionDTO> consumos) {
        this.tipo = tipo;
        this.fecha = fecha;
        this.cantidad = cantidad;
        this.descripcion = descripcion;
        this.usuario = usuario;
        this.documentoId = documentoId;
        this.saldo = saldo;
        this.lote = lote;
        this.consumos = consumos;
    }

    public String getTipo() { return tipo; }
    public LocalDate getFecha() { return fecha; }
    public double getCantidad() { return cantidad; }
    public String getDescripcion() { return descripcion; }
    public String getUsuario() { return usuario; }
    public Long getDocumentoId() { return documentoId; }
    public double getSaldo() { return saldo; }
    public String getLote() { return lote; }
    public List<ConsumoElaboracionDTO> getConsumos() { return consumos; }
}

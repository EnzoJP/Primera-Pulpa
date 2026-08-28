package com.primeraPulpa.controller;

import com.primeraPulpa.Services.MateriaPrimaService;
import com.primeraPulpa.Services.MixService;
import com.primeraPulpa.Services.MovimientoStockService;
import com.primeraPulpa.Services.MovimientoStockService.ListaMovimientos;
import com.primeraPulpa.exceptions.ErrorServiceException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * HU-15 y HU-16: consulta de historial de movimientos que afectaron el stock
 * de una materia prima (StockMP) y de un mix (StockMix), con filtro por entidad
 * y rango de fechas.
 */
@Controller
@RequestMapping("/stock")
public class MovimientoStockController {

    private final MovimientoStockService movimientoStockService;
    private final MateriaPrimaService materiaPrimaService;
    private final MixService mixService;

    public MovimientoStockController(MovimientoStockService movimientoStockService,
                                     MateriaPrimaService materiaPrimaService,
                                     MixService mixService) {
        this.movimientoStockService = movimientoStockService;
        this.materiaPrimaService = materiaPrimaService;
        this.mixService = mixService;
    }

    // ── HU-15: histórico de StockMP ──────────────────────────────────────────
    @GetMapping("/stock-mp")
    public String historialStockMP(@RequestParam(value = "mpId", required = false) Long mpId,
                                   @RequestParam(value = "desde", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam(value = "hasta", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   Model model) {
        ListaMovimientos resultado = (mpId != null)
                ? movimientoStockService.historialStockMP(mpId, desde, hasta)
                : ListaMovimientos.vacia();
        model.addAttribute("resultado", resultado);
        model.addAttribute("mpIdFiltro", mpId);
        model.addAttribute("desdeFiltro", desde);
        model.addAttribute("hastaFiltro", hasta);
        model.addAttribute("materiasPrimas", listarMateriasPrimas());
        return "stock/stock-mp";
    }

    // ── HU-16: histórico de StockMix ─────────────────────────────────────────
    @GetMapping("/stock-mix")
    public String historialStockMix(@RequestParam(value = "mixId", required = false) Long mixId,
                                    @RequestParam(value = "desde", required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                    @RequestParam(value = "hasta", required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                    Model model) {
        ListaMovimientos resultado = (mixId != null)
                ? movimientoStockService.historialStockMix(mixId, desde, hasta)
                : ListaMovimientos.vacia();
        model.addAttribute("resultado", resultado);
        model.addAttribute("mixIdFiltro", mixId);
        model.addAttribute("desdeFiltro", desde);
        model.addAttribute("hastaFiltro", hasta);
        model.addAttribute("mixes", listarMixes());
        return "stock/stock-mix";
    }

    private List<com.primeraPulpa.entities.MateriaPrima> listarMateriasPrimas() {
        try {
            return materiaPrimaService.listarActivos();
        } catch (ErrorServiceException e) {
            return List.of();
        }
    }

    private List<com.primeraPulpa.entities.Mix> listarMixes() {
        try {
            return mixService.listarActivos();
        } catch (ErrorServiceException e) {
            return List.of();
        }
    }
}


package com.primeraPulpa.controller;

import com.primeraPulpa.dto.DetalleEstadisticaMesDTO;
import com.primeraPulpa.dto.EstadisticaAnualDTO;
import com.primeraPulpa.dto.EstadisticaMensualDTO;
import com.primeraPulpa.Services.EstadisticasService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Month;
import java.time.YearMonth;
import java.util.List;

@Controller
public class EstadisticasController {

    public record MesOption(int numero, String nombre) {
    }

    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    @GetMapping("/estadisticas")
    public String index(@RequestParam(value = "anio", required = false) Integer anio,
                        @RequestParam(value = "mes", required = false) Integer mes,
                        Model model) {
        YearMonth seleccion = YearMonth.now();
        if (anio != null && mes != null) {
            seleccion = YearMonth.of(anio, mes);
        }
        final YearMonth seleccionFinal = seleccion;

        List<EstadisticaMensualDTO> mensuales = estadisticasService.calcularMensuales(
                seleccionFinal.getYear());

        EstadisticaMensualDTO actual = mensuales.stream()
                .filter(m -> m.anio() == seleccionFinal.getYear() && m.mes() == seleccionFinal.getMonthValue())
                .findFirst()
                .orElse(null);

        List<MesOption> meses = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(n -> new MesOption(n, capitalizar(Month.of(n).name())))
                .toList();

        model.addAttribute("mensuales", mensuales);
        model.addAttribute("actual", actual);
        model.addAttribute("anio", seleccionFinal.getYear());
        model.addAttribute("mes", seleccionFinal.getMonthValue());
        model.addAttribute("meses", meses);
        return "estadisticas/index";
    }

    // ── Desglose mensual por mix ─────────────────────────────────────────────
    @GetMapping("/estadisticas/mes/{anio}/{mes}")
    public String detalleMes(@PathVariable int anio,
                             @PathVariable int mes,
                             Model model) {
        List<DetalleEstadisticaMesDTO> detalles = estadisticasService.desgloseMensualPorMix(anio, mes);

        double totalKg = detalles.stream().mapToDouble(DetalleEstadisticaMesDTO::cantidadVendida).sum();
        double totalFacturado = detalles.stream().mapToDouble(DetalleEstadisticaMesDTO::facturado).sum();
        double totalCosto = detalles.stream().mapToDouble(DetalleEstadisticaMesDTO::costo).sum();
        double totalGanancia = detalles.stream().mapToDouble(DetalleEstadisticaMesDTO::ganancia).sum();

        model.addAttribute("detalles", detalles);
        model.addAttribute("anio", anio);
        model.addAttribute("mes", mes);
        model.addAttribute("nombreMes", estadisticasService.nombreMes(mes));
        model.addAttribute("totalKg", totalKg);
        model.addAttribute("totalFacturado", totalFacturado);
        model.addAttribute("totalCosto", totalCosto);
        model.addAttribute("totalGanancia", totalGanancia);
        return "estadisticas/mes";
    }

    // ── Resumen anual ────────────────────────────────────────────────────────
    @GetMapping("/estadisticas/anual/{anio}")
    public String detalleAnual(@PathVariable int anio, Model model) {
        EstadisticaAnualDTO resumen = estadisticasService.resumenAnual(anio);
        List<DetalleEstadisticaMesDTO> detalles = estadisticasService.desgloseAnualPorMix(anio);

        double totalKg = detalles.stream().mapToDouble(DetalleEstadisticaMesDTO::cantidadVendida).sum();
        double totalFacturado = detalles.stream().mapToDouble(DetalleEstadisticaMesDTO::facturado).sum();
        double totalCosto = detalles.stream().mapToDouble(DetalleEstadisticaMesDTO::costo).sum();
        double totalGanancia = detalles.stream().mapToDouble(DetalleEstadisticaMesDTO::ganancia).sum();

        model.addAttribute("resumen", resumen);
        model.addAttribute("detalles", detalles);
        model.addAttribute("totalKg", totalKg);
        model.addAttribute("totalFacturado", totalFacturado);
        model.addAttribute("totalGanancia", totalGanancia);
        model.addAttribute("anio", anio);
        return "estadisticas/anual";
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        String lower = texto.toLowerCase();
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }
}

package com.primeraPulpa.controller;

import com.primeraPulpa.dto.DashboardDTO;
import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.Services.MateriaPrimaService;
import com.primeraPulpa.Services.MixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired
    private MateriaPrimaService materiaPrimaService;

    @Autowired
    private MixService mixService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        List<MateriaPrima> materiasPrimas = materiaPrimaService.listarActivos();

        // 1. Valor total del inventario: sum(precio * cantidadActual)
        BigDecimal valorTotal = materiasPrimas.stream()
                .map(mp -> BigDecimal.valueOf(mp.getPrecio() * mp.getCantidadActual()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Alertas de bajo stock (cantidadActual <= cantidadMinima)
                List<DashboardDTO.ItemStockBajoDTO> bajoStock = materiasPrimas.stream()
                        .filter(mp -> mp.getCantidadActual() <= mp.getCantidadMinima())
                        .map(mp -> new DashboardDTO.ItemStockBajoDTO(
                                mp.getId(),
                                mp.getNombre(),
                                mp.getCantidadActual(),
                                mp.getCantidadMinima(),
                                mp.getUnidadMedida() != null ? mp.getUnidadMedida().getDescripcion() : "u"
                        ))
                        .limit(5)
                        .collect(Collectors.toList());

        // 3. Últimos ingresos registrados
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                List<DashboardDTO.UltimoIngresoDTO> ultimosIngresos = materiasPrimas.stream()
                        .sorted((a, b) -> {
                            if (a.getFechaIngreso() == null) return 1;
                            if (b.getFechaIngreso() == null) return -1;
                            return b.getFechaIngreso().compareTo(a.getFechaIngreso());
                        })
                        .limit(5)
                        .map(mp -> new DashboardDTO.UltimoIngresoDTO(
                                mp.getId(),
                                mp.getNombre(),
                                mp.getCantidadActual(),
                                mp.getUnidadMedida() != null ? mp.getUnidadMedida().getDescripcion() : "u",
                                mp.getFechaIngreso() != null ? mp.getFechaIngreso().format(formatter) : "—"
                        ))
                        .collect(Collectors.toList());


        // Obtener top 5 insumos con mayor stock actual
        List<MateriaPrima> topStock = materiasPrimas.stream()
                .sorted((a, b) -> Double.compare(b.getCantidadActual(), a.getCantidadActual()))
                .limit(5)
                .collect(Collectors.toList());

        List<String> labels = topStock.stream()
                .map(MateriaPrima::getNombre)
                .collect(Collectors.toList());

        List<Double> data = topStock.stream()
                .map(MateriaPrima::getCantidadActual)
                .collect(Collectors.toList());
        DashboardDTO stats = new DashboardDTO();
        stats.setChartLabels(labels);
        stats.setChartData(data);
        stats.setValorTotalInventario(valorTotal);
        stats.setTotalMateriasPrimas(materiasPrimas.size());
        stats.setTotalMixes(mixService.listarActivos().size());
        stats.setTotalAlertasStockBajo(bajoStock.size());
        stats.setAlertasStockBajo(bajoStock);
        stats.setUltimosIngresos(ultimosIngresos);

        model.addAttribute("stats", stats);
        return "dashboard/dashboard";
    }
}
package com.primeraPulpa.controller;

import com.primeraPulpa.dto.IngresoMPMapper;
import com.primeraPulpa.dto.IngresoResumenDTO;
import com.primeraPulpa.Services.IngresoMPService;
import com.primeraPulpa.entities.DetalleIngresoMP;
import com.primeraPulpa.entities.IngresoMP;
import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.IngresoMPRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/ingresos-mp")
public class IngresoMPController {

    private final IngresoMPService ingresoMPService;
    private final IngresoMPRepository ingresoMPRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final IngresoMPMapper ingresoMPMapper;

    public IngresoMPController(IngresoMPService ingresoMPService,
                               IngresoMPRepository ingresoMPRepository,
                               MateriaPrimaRepository materiaPrimaRepository,
                               IngresoMPMapper ingresoMPMapper) {
        this.ingresoMPService = ingresoMPService;
        this.ingresoMPRepository = ingresoMPRepository;
        this.materiaPrimaRepository = materiaPrimaRepository;
        this.ingresoMPMapper = ingresoMPMapper;
    }

    // ── Listado ──────────────────────────────────────────────────────────────
    @GetMapping("")
    public String listado(Model model, HttpServletRequest request) {
        List<IngresoMP> ingresos = ingresoMPRepository.findAll().stream()
                .filter(i -> !Boolean.TRUE.equals(i.getEliminado()))
                .toList();
        List<IngresoResumenDTO> resumenes = ingresoMPMapper.toResumenList(ingresos);
        model.addAttribute("items", resumenes);
        model.addAttribute("currentUri", request.getRequestURI());
        return "ingreso-mp/list";
    }

    // ── Formulario nuevo ─────────────────────────────────────────────────────
    @GetMapping("/nuevo")
    public String formularioNuevo(Model model, HttpServletRequest request) {
        List<MateriaPrima> materiaPrimas = materiaPrimaRepository.findAll().stream()
                .filter(mp -> !Boolean.TRUE.equals(mp.getEliminado()))
                .toList();
        model.addAttribute("materiaPrimas", materiaPrimas);
        model.addAttribute("currentUri", request.getRequestURI());
        return "ingreso-mp/form";
    }

    // ── Confirmar ingreso ─────────────────────────────────────────────────────
    @PostMapping("/confirmar")
    public String confirmar(
            @RequestParam("materiaPrimaId") List<Long> mpIds,
            @RequestParam("cantidad") List<Double> cantidades,
            @RequestParam("costoUnitario") List<Double> costos,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            List<DetalleIngresoMP> detalles = new ArrayList<>();
            for (int i = 0; i < mpIds.size(); i++) {
                // Omitir filas donde la materia prima no fue seleccionada
                if (mpIds.get(i) == null || mpIds.get(i) == 0) continue;

                MateriaPrima mp = materiaPrimaRepository.findById(mpIds.get(i))
                        .orElseThrow(() -> new ErrorServiceException("Materia prima no encontrada"));

                DetalleIngresoMP detalle = new DetalleIngresoMP();
                detalle.setMateriaPrima(mp);
                detalle.setCantidad(cantidades.get(i));
                detalle.setCostoUnitario(costos.get(i));
                detalles.add(detalle);
            }

            ingresoMPService.registrar(detalles);
            redirectAttributes.addFlashAttribute("success", "Ingreso registrado correctamente. El stock fue actualizado.");
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar el ingreso.");
        }
        return "redirect:/ingresos-mp";
    }

    // ── Ver detalle de un ingreso ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, HttpServletRequest request) {
        try {
            IngresoMP ingreso = ingresoMPRepository.findById(id)
                    .orElseThrow(() -> new ErrorServiceException("Ingreso no encontrado"));
            model.addAttribute("ingreso", ingreso);
            model.addAttribute("currentUri", request.getRequestURI());
            return "ingreso-mp/detail";
        } catch (Exception e) {
            model.addAttribute("error", "Ingreso no encontrado");
            return "error/500";
        }
    }
}

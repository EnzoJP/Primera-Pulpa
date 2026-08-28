package com.primeraPulpa.controller;

import com.primeraPulpa.dto.IngresoMPMapper;
import com.primeraPulpa.dto.IngresoResumenDTO;
import com.primeraPulpa.Services.IngresoMPService;
import com.primeraPulpa.entities.DetalleIngresoMP;
import com.primeraPulpa.entities.IngresoMP;
import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.IngresoMPRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import com.primeraPulpa.repositories.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/ingresos-mp")
public class IngresoMPController {

    private static final int PAGE_SIZE = 30;

    private final IngresoMPService ingresoMPService;
    private final IngresoMPRepository ingresoMPRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final IngresoMPMapper ingresoMPMapper;
    private final UsuarioRepository usuarioRepository;

    public IngresoMPController(IngresoMPService ingresoMPService,
                               IngresoMPRepository ingresoMPRepository,
                               MateriaPrimaRepository materiaPrimaRepository,
                               IngresoMPMapper ingresoMPMapper,
                               UsuarioRepository usuarioRepository) {
        this.ingresoMPService = ingresoMPService;
        this.ingresoMPRepository = ingresoMPRepository;
        this.materiaPrimaRepository = materiaPrimaRepository;
        this.ingresoMPMapper = ingresoMPMapper;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Listado con filtro por fecha y paginación ─────────────────────────────
    @GetMapping("")
    public String listado(@RequestParam(value = "fecha", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                          @RequestParam(value = "page", defaultValue = "0") int page,
                          Model model, HttpServletRequest request) {
        List<IngresoMP> todos = ingresoMPRepository.findAllByEliminadoFalseOrderByFechaHoraDescIdDesc().stream()
                .filter(i -> fecha == null || i.getFechaHora() != null && i.getFechaHora().toLocalDate().equals(fecha))
                .toList();

        List<IngresoResumenDTO> resumenes = ingresoMPMapper.toResumenList(todos);

        int total = resumenes.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int paginaActual = Math.max(0, Math.min(page, totalPages - 1));
        int desde = paginaActual * PAGE_SIZE;
        int hasta = Math.min(desde + PAGE_SIZE, total);
        List<IngresoResumenDTO> pagina = desde < total ? resumenes.subList(desde, hasta) : List.of();

        model.addAttribute("items", pagina);
        model.addAttribute("cantidadIngresada", todos.stream().mapToDouble(IngresoMP::getCantidadIngresda).sum());
        model.addAttribute("fechaFiltro", fecha);
        model.addAttribute("page", paginaActual);
        model.addAttribute("totalPages", totalPages);
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
            @RequestParam(value = "fechaVencimiento", required = false) List<String> vencimientos,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            Usuario usuario = usuarioRepository.findByEmail(user.getUsername())
                    .orElseThrow(() -> new ErrorServiceException("Usuario no encontrado"));

            List<DetalleIngresoMP> detalles = new ArrayList<>();
            for (int i = 0; i < mpIds.size(); i++) {
                // Omitir filas donde la materia prima no fue seleccionada
                if (mpIds.get(i) == null || mpIds.get(i) == 0) continue;

                MateriaPrima mp = materiaPrimaRepository.findById(mpIds.get(i))
                        .orElseThrow(() -> new ErrorServiceException("Materia prima no encontrada"));

                DetalleIngresoMP detalle = new DetalleIngresoMP();
                detalle.setMateriaPrima(mp);
                detalle.setCantidad(cantidades.get(i));
                detalle.setFechaVencimiento(parseFechaVencimiento(vencimientos, i));
                detalles.add(detalle);
            }

            ingresoMPService.registrar(detalles, usuario);
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

    private LocalDate parseFechaVencimiento(List<String> vencimientos, int index) {
        if (vencimientos == null || index >= vencimientos.size()) {
            return null;
        }
        String valor = vencimientos.get(index);
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
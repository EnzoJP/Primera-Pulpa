package com.primeraPulpa.controller;

import com.primeraPulpa.Services.LoteMixService;
import com.primeraPulpa.Services.MixService;
import com.primeraPulpa.dto.LoteDiarioDTO;
import com.primeraPulpa.entities.LoteMix;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.UsuarioRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/elaboracion")
public class ElaboracionController {

    private static final int PAGE_SIZE = 30;

    private final LoteMixService loteMixService;
    private final MixService mixService;
    private final UsuarioRepository usuarioRepository;

    public ElaboracionController(LoteMixService loteMixService, MixService mixService,
                                 UsuarioRepository usuarioRepository) {
        this.loteMixService = loteMixService;
        this.mixService = mixService;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Listado paginado de lotes por día, con búsqueda por fecha ────────────
    @GetMapping("")
    public String listado(@RequestParam(value = "fecha", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                          @RequestParam(value = "page", defaultValue = "0") int page,
                          Model model) {
        List<LoteDiarioDTO> todos = loteMixService.listarLotesPorDia();
        if (fecha != null) {
            todos = todos.stream().filter(l -> l.getFecha().equals(fecha)).toList();
        }

        int totalLotes = todos.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalLotes / (double) PAGE_SIZE));
        int paginaActual = Math.max(0, Math.min(page, totalPages - 1));
        int desde = paginaActual * PAGE_SIZE;
        int hasta = Math.min(desde + PAGE_SIZE, totalLotes);
        List<LoteDiarioDTO> pagina = desde < totalLotes ? todos.subList(desde, hasta) : List.of();

        model.addAttribute("lotes", pagina);
        model.addAttribute("page", paginaActual);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("fechaFiltro", fecha);
        return "elaboracion/list";
    }

    // ── Formulario nuevo ─────────────────────────────────────────────────────
    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        LoteMix lote = new LoteMix();
        lote.setFechaElaboracion(LocalDate.now());
        model.addAttribute("lote", lote);
        cargarMixes(model);
        model.addAttribute("formTitle", "Cargar Elaboración");
        model.addAttribute("formAction", "/elaboracion/registrar");
        return "elaboracion/form";
    }

    // ── Confirmar elaboración (acumula al lote del mix+día si ya existe) ─────
    @PostMapping("/registrar")
    public String registrar(@RequestParam("mixId") Long mixId,
                            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                            @RequestParam("cantidad") Double cantidad,
                            @AuthenticationPrincipal User user,
                            RedirectAttributes redirectAttributes) {
        try {
            Mix mix = mixService.getOne(mixId);
            Usuario usuario = usuarioRepository.findByEmail(user.getUsername())
                    .orElseThrow(() -> new ErrorServiceException("Usuario no encontrado"));
            loteMixService.registrarElaboracion(mix, fecha, cantidad, usuario);
            redirectAttributes.addFlashAttribute("success", "Elaboración registrada correctamente.");
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar la elaboración.");
        }
        return "redirect:/elaboracion";
    }

    // ── Detalle del lote completo de una fecha (todos los mixes del día) ─────
    @GetMapping("/dia/{fecha}")
    public String detalleDia(@PathVariable("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                             Model model) {
        List<LoteMix> loteDelDia = loteMixService.listarDelDia(fecha);
        if (loteDelDia.isEmpty()) {
            model.addAttribute("error", "No existe un lote para la fecha indicada");
            return "error/404";
        }
        double totalDia = loteDelDia.stream()
                .mapToDouble(l -> l.getCantidadElaborada() != null ? l.getCantidadElaborada() : 0.0)
                .sum();
        model.addAttribute("fecha", fecha);
        model.addAttribute("loteDelDia", loteDelDia);
        model.addAttribute("totalDia", totalDia);
        return "elaboracion/detail";
    }

    // ── Formulario editar ────────────────────────────────────────────────────
    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        try {
            LoteMix lote = loteMixService.getOne(id);
            model.addAttribute("lote", lote);
            cargarMixes(model);
            model.addAttribute("formTitle", "Editar Elaboración");
            model.addAttribute("formAction", "/elaboracion/" + id);
            return "elaboracion/form";
        } catch (ErrorServiceException e) {
            model.addAttribute("error", "Elaboración no encontrada");
            return "error/404";
        }
    }

    // ── Actualizar elaboración ───────────────────────────────────────────────
    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @RequestParam("mixId") Long mixId,
                             @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                             @RequestParam("cantidad") Double cantidad,
                             RedirectAttributes redirectAttributes) {
        try {
            Mix mix = mixService.getOne(mixId);
            LoteMix lote = new LoteMix();
            lote.setMix(mix);
            lote.setFechaElaboracion(fecha);
            lote.setCantidadElaborada(cantidad);
            loteMixService.modificar(id, lote);
            redirectAttributes.addFlashAttribute("success", "Elaboración actualizada correctamente.");
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar la elaboración.");
        }
        return "redirect:/elaboracion";
    }

    // ── Eliminar elaboración ─────────────────────────────────────────────────
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            loteMixService.bajaLogica(id);
            redirectAttributes.addFlashAttribute("success", "Elaboración eliminada correctamente.");
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la elaboración.");
        }
        return "redirect:/elaboracion";
    }

    private void cargarMixes(Model model) {
        try {
            model.addAttribute("mixes", mixService.listarActivos().stream()
                    .sorted(Mix.ordenarPorPresentacion())
                    .toList());
        } catch (ErrorServiceException e) {
            model.addAttribute("mixes", List.of());
        }
    }
}
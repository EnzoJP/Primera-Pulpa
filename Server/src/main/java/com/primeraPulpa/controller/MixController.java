package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.Services.MixService;
import com.primeraPulpa.exceptions.ErrorServiceException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping(path = "/mixes")
public class MixController extends BaseController<Mix, Long> {
    private final MixService service;

    public MixController(MixService service) {
        super(service, Mix.class, "/mixes", "mix");
        this.service = service;
    }

    @Override
    protected String vistaFormulario() {
        return "mix/form";
    }

    @Override
    protected String vistaListado() {
        return "mix/list";
    }

    @Override
    @GetMapping("")
    public String getAll(@RequestParam Map<String, String> params, Model model) {
        try {
            List<Mix> activos = service.listarActivos();
            if (!params.isEmpty()) {
                activos = filtrarPorParametros(activos, params);
            }
            Map<Long, Double> pendientes = service.cantidadesPendientesPorMix();

            // Verificar si algún mix tiene stock insuficiente para cubrir pedidos pendientes
            boolean hayStockInsuficiente = activos.stream().anyMatch(m -> {
                double pendiente = pendientes.getOrDefault(m.getId(), 0.0);
                return pendiente > 0 && m.getStock() < pendiente;
            });

            cargarAtributosBase(model);
            model.addAttribute("items", activos);
            model.addAttribute("cantidadesPendientes", pendientes);
            model.addAttribute("hayStockInsuficiente", hayStockInsuficiente);
            return vistaListado();
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            return "error/500";
        } catch (Exception e) {
            model.addAttribute("error", "Error de Sistema");
            return "error/500";
        }
    }

    @Override
    protected String vistaDetalle() {
        return "mix/detail";
    }

    protected List<Mix> filtrarPorNombre(List<Mix> lista, Map<String, String> params) {
        if (params.containsKey("nombre") && params.get("nombre") != null && !params.get("nombre").trim().isEmpty()) {
            String termino = params.get("nombre").toLowerCase().trim();
            return lista.stream()
                    .filter(m -> m.getNombre() != null && m.getNombre().toLowerCase().contains(termino))
                    .collect(Collectors.toList());
        }
        return lista;
    }


    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("item") Mix entidad,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        String basePath = "/mixes";
        String entityName = "Mix";
        if (bindingResult.hasErrors()) {
            cargarAtributosBase(model);
            cargarDatosFormulario(model);
            model.addAttribute("formAction", basePath + "/" + id);
            model.addAttribute("formTitle", "Editar " + entityName);
            return vistaFormulario();
        }

        try {
            preUpdate(entidad);
            Optional<Mix> actualizado = service.modificar(id, entidad);
            if (actualizado.isPresent()) {
                postUpdate(actualizado.get());
                redirectAttributes.addFlashAttribute("success", entityName + " actualizado correctamente");
                service.recalcularCosto(actualizado.get().getId());
                return "redirect:" + basePath;
            } else {
                model.addAttribute("error", "Entidad no encontrada");
                return "error/404";
            }
        } catch (ErrorServiceException e) {
            cargarAtributosBase(model);
            cargarDatosFormulario(model);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("formAction", basePath + "/" + id);
            model.addAttribute("formTitle", "Editar " + entityName);
            return vistaFormulario();
        } catch (Exception e) {
            cargarAtributosBase(model);
            cargarDatosFormulario(model);
            model.addAttribute("error", "Error al actualizar la entidad");
            model.addAttribute("formAction", basePath + "/" + id);
            model.addAttribute("formTitle", "Editar " + entityName);
            return vistaFormulario();
        }
    }
}

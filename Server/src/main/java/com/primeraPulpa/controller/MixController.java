package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.Services.MixService;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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
    private final UsuarioRepository usuarioRepository;

    public MixController(MixService service, UsuarioRepository usuarioRepository) {
        super(service, Mix.class, "/mixes", "mix");
        this.service = service;
        this.usuarioRepository = usuarioRepository;
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

    @Override
    public String getOne(@PathVariable Long id, Model model) {
        try {
            Mix entidad = service.getOne(id);
            cargarAtributosBase(model);
            model.addAttribute("item", entidad);
            model.addAttribute("desglose", service.desgloseCostos(id));
            model.addAttribute("historialPrecios", service.obtenerHistorialPrecio(id));
            return vistaDetalle();
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            return "error/404";
        } catch (Exception e) {
            model.addAttribute("error", "Entidad no encontrada");
            return "error/404";
        }
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


    @Override
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
            // Obtener precio anterior antes de actualizar
            aplicarPrecioPorUnidad(entidad);
            Mix mixAnterior = service.getOne(id);
            Double precioAnterior = mixAnterior != null ? mixAnterior.getPrecioVenta() : null;

            preUpdate(entidad);
            Optional<Mix> actualizado = service.modificar(id, entidad);
            if (actualizado.isPresent()) {
                // Registrar cambio de precio si cambió (HU-8)
                Double precioNuevo = actualizado.get().getPrecioVenta();
                Usuario usuario = obtenerUsuarioActual();
                service.registrarCambioPrecio(actualizado.get(), precioAnterior, precioNuevo, usuario);

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

    // Convierte el precio POR UNIDAD (que carga el formulario) a precio por kg
    // antes de persistir, usando el tamaño de presentación del mix.
    private void aplicarPrecioPorUnidad(Mix mix) {
        if (mix.getPrecioVentaUnidad() != null && mix.getCantidadPorUnidadOrDefault() > 0) {
            double porKg = Math.round((mix.getPrecioVentaUnidad() / mix.getCantidadPorUnidadOrDefault()) * 100.0) / 100.0;
            mix.setPrecioVenta(porKg);
        }
    }

    @Override
    protected void preCreate(Mix entidad) throws ErrorServiceException {
        aplicarPrecioPorUnidad(entidad);
    }

    private Usuario obtenerUsuarioActual() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User u) {
                return usuarioRepository.findByEmail(u.getUsername()).orElse(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

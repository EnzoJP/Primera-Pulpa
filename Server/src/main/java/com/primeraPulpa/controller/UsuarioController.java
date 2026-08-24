package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.Services.RolService;
import com.primeraPulpa.Services.UsuarioService;
import com.primeraPulpa.exceptions.ErrorServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(path = "/usuarios")
public class UsuarioController extends BaseController<Usuario, Long> {

    private final UsuarioService usuarioService;
    private final RolService rolService;

    public UsuarioController(UsuarioService service, RolService rolService) {
        super(service, Usuario.class, "/usuarios", "usuario");
        this.usuarioService = service;
        this.rolService = rolService;
    }

    @GetMapping("")
    @Override
    public String getAll(@RequestParam Map<String, String> params, Model model) {
        try {
            List<Usuario> todos = usuarioService.listarTodos().stream()
                    .sorted(Comparator.comparing(Usuario::getId))
                    .toList();

            if (!params.isEmpty()) {
                todos = filtrarPorParametros(todos, params);
            }

            cargarAtributosBase(model);
            model.addAttribute("items", todos);
            return vistaListado();
        } catch (Exception e) {
            model.addAttribute("error", "Error al listar los usuarios");
            return "error/500";
        }
    }

    @Override
    protected void cargarDatosFormulario(Model model) {
        try {
            model.addAttribute("roles", rolService.listarActivos());
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar los roles");
        }
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.reactivar(id);
            redirectAttributes.addFlashAttribute("success", "Usuario reactivado correctamente.");
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al reactivar el usuario.");
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam("newPassword") String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            usuarioService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("success", "Contraseña restablecida correctamente.");
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al restablecer la contraseña.");
        }
        return "redirect:/usuarios";
    }

    @Override
    protected String vistaListado() {
        return "usuario/list";
    }

    @Override
    protected String vistaFormulario() {
        return "usuario/form";
    }

    @Override
    protected String vistaDetalle() {
        return "usuario/detail";
    }
}

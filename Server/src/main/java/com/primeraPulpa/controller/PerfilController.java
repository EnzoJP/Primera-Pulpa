package com.primeraPulpa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import com.primeraPulpa.Services.UsuarioService;
import com.primeraPulpa.entities.Usuario;

@Controller
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/perfil")
    public String perfil(Model model, HttpServletRequest request, @AuthenticationPrincipal User user) {
        if (user != null) {
            Usuario usuario = usuarioService.findByEmail(user.getUsername()).orElse(null);
            model.addAttribute("usuario", usuario);
        }
        model.addAttribute("currentUri", request.getRequestURI());
        return "perfil/index";
    }
}

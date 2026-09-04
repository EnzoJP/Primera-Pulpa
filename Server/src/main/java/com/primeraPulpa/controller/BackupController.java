package com.primeraPulpa.controller;

import com.primeraPulpa.Services.BackupRestoreService;
import com.primeraPulpa.exceptions.ErrorServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/backups")
public class BackupController {

    private final BackupRestoreService backupRestoreService;

    public BackupController(BackupRestoreService backupRestoreService) {
        this.backupRestoreService = backupRestoreService;
    }

    @GetMapping("")
    public String listar(Model model) {
        model.addAttribute("backups", backupRestoreService.listarBackupsDisponibles());
        return "backup/list";
    }

    @PostMapping("/restaurar")
    public String restaurar(@RequestParam("archivo") String archivo,
                            RedirectAttributes redirectAttributes,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            Authentication authentication) {
        try {
            backupRestoreService.restaurarBackup(archivo);

            // Restauración ok: forzar cierre de sesión para que todos los usuarios
            // (incluido el admin) vuelvan a autenticarse contra los datos restaurados.
            if (authentication != null) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
            }
            redirectAttributes.addFlashAttribute("restaurado", "Base de datos restaurada correctamente desde " + archivo + ". Volvé a iniciar sesión.");
            return "redirect:/login";
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error inesperado al restaurar la base de datos.");
        }
        return "redirect:/backups";
    }
}

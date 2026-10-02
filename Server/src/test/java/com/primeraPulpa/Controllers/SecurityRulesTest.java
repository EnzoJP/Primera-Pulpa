package com.primeraPulpa.Controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    // ── 1. Recursos públicos y estáticos (permitAll) ──────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/css/style.css", "/js/app.js"})
    @DisplayName("Recursos públicos deben responder sin redirección a login ni 403 para usuarios anónimos")
    void recursosPublicos_permitenAccesoAnonimo(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(302)));
    }

    // ── 2. Usuario no autenticado ───────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "/usuarios",
            "/estadisticas",
            "/clientes",
            "/mixes/nuevo",
            "/dashboard"
    })
    @DisplayName("Usuario anónimo debe ser redirigido a /login al intentar acceder a rutas privadas")
    void anonimo_esRedirigidoAlLogin(String urlPrivada) throws Exception {
        mockMvc.perform(get(urlPrivada))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ── 3. EMPLEADO intentando acceder a rutas exclusivas de ADMIN (403) ────

    @ParameterizedTest
    @ValueSource(strings = {
            "/usuarios",
            "/usuarios/nuevo",
            "/backups",
            "/formulas/nuevo",
            "/formulas/1/editar",
            "/materias-primas/1/editar",
            "/materias-primas/nuevo",
            "/materias-primas/1/eliminar",
            "/unidades-medida",
            "/mixes/nuevo",
            "/mixes/1/editar",
            "/elaboracion/1/editar",
            "/pedidos/nuevo",
            "/pedidos/1/editar",
            "/estadisticas",
            "/clientes",
            "/costos-adicionales"
    })
    @WithMockUser(username = "empleado@primera.com", roles = {"EMPLEADO"})
    @DisplayName("Empleado debe recibir 403 Forbidden en todas las rutas exclusivas de ADMIN")
    void empleado_noPuedeAccederARutasAdmin(String urlAdmin) throws Exception {
        mockMvc.perform(get(urlAdmin))
                .andExpect(status().isForbidden());
    }

    // ── 4. Rutas comunes permitidas a EMPLEADO (anyRequest -> ADMIN o EMPLEADO)

    @Test
    @WithMockUser(username = "empleado@primera.com", roles = {"EMPLEADO"})
    @DisplayName("Empleado no debe recibir 403 en rutas generales como dashboard o listados permitidos")
    void empleado_puedeAccederARutasComunes() throws Exception {
        // En /dashboard no debe dar 403 Forbidden
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is(not(403)));
    }

    // ── 5. ADMIN accediendo a sus rutas (no debe dar 403) ───────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "/usuarios",
            "/backups",
            "/estadisticas",
            "/clientes",
            "/unidades-medida",
            "/mixes/nuevo"
    })
    @WithMockUser(username = "admin@primera.com", roles = {"ADMIN"})
    @DisplayName("Admin no debe ser bloqueado con 403 en sus rutas asignadas")
    void admin_puedeAccederARutasAdmin(String urlAdmin) throws Exception {
        mockMvc.perform(get(urlAdmin))
                .andExpect(status().is(not(403)));
    }
}
package com.primeraPulpa.configs;

import com.primeraPulpa.Services.MixService;
import com.primeraPulpa.Services.RolService;
import com.primeraPulpa.Services.UsuarioService;
import com.primeraPulpa.entities.*;
import com.primeraPulpa.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            UsuarioService usuarioService,
            RolService rolService,
            UnidadMedidaRepository unidadMedidaRepository,
            CostoAdicionalRepository costoAdicionalRepository,
            MateriaPrimaRepository materiaPrimaRepository,
            IngresoMPRepository ingresoMPRepository,
            DetalleIngresoMPRepository detalleIngresoMPRepository,
            MixRepository mixRepository,
            MixService mixService,
            FormulaRepository formulaRepository,
            DetalleFormulaRepository detalleFormulaRepository,
            ClienteRepository clienteRepository,
            PedidoRepository pedidoRepository,
            DetallePedidoRepository detallePedidoRepository,
            LoteMixRepository loteMixRepository) {

        return args -> {
            if (usuarioRepository.count() > 0) {
                return;
            }
            logger.info("--- INICIALIZANDO DATOS DE PRUEBA EN PRIMERA PULPA ---");

            // 1. Unidades de Medida
            UnidadMedida umKg = unidadMedidaRepository.save(UnidadMedida.builder().descripcion("kg").build());
            unidadMedidaRepository.save(UnidadMedida.builder().descripcion("gramos").build());

            // 2. Roles
            Rol rolAdmin = rolService.alta(Rol.builder().descripcion("ADMIN").build());
            Rol rolEmpleado = rolService.alta(Rol.builder().descripcion("EMPLEADO").build());

            // 3. Usuarios
            usuarioService.alta(Usuario.builder()
                    .nombre("Aaa")
                    .email("Aaa@gmail.com")
                    .passwordHash("Aaa123")
                    .rol(rolAdmin)
                    .build());
        };
    }
}

package com.primeraPulpa.configs;

import com.primeraPulpa.Services.ClienteService;
import com.primeraPulpa.Services.RolService;
import com.primeraPulpa.Services.UsuarioService;
import com.primeraPulpa.entities.Rol;
import com.primeraPulpa.entities.UnidadMedida;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.repositories.ClienteRepository;
import com.primeraPulpa.repositories.UnidadMedidaRepository;
import com.primeraPulpa.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

  private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

  @Bean
  public CommandLineRunner initDatabase(UsuarioRepository usuarioRepository,
                                        PasswordEncoder passwordEncoder,
                                        UsuarioService usuarioService,
                                        RolService rolService,
                                        UnidadMedidaRepository unidadMedidaRepository) {

    return args -> {
      // Unidades de medida iniciales
      if (unidadMedidaRepository.count() == 0) {
        unidadMedidaRepository.save(UnidadMedida.builder().descripcion("kg").build());
        unidadMedidaRepository.save(UnidadMedida.builder().descripcion("litros").build());
        unidadMedidaRepository.save(UnidadMedida.builder().descripcion("unidades").build());
        logger.info("Unidades de medida iniciales creadas");
      }

      // Roles iniciales
      Rol rolAdmin = null;
      Rol rolEmpleado = null;

      for (Rol r : rolService.listarActivos()) {
          if ("ADMIN".equals(r.getDescripcion())) rolAdmin = r;
          if ("EMPLEADO".equals(r.getDescripcion())) rolEmpleado = r;
      }

      if (rolAdmin == null) {
          rolAdmin = rolService.alta(Rol.builder().descripcion("ADMIN").build());
          logger.info("Rol ADMIN creado");
      }
      if (rolEmpleado == null) {
          rolEmpleado = rolService.alta(Rol.builder().descripcion("EMPLEADO").build());
          logger.info("Rol EMPLEADO creado");
      }

      // Check if the admin user already exists
      if (usuarioRepository.findByEmail("admin@example.com").isEmpty()) {
          usuarioService.alta(Usuario.builder()
              .nombre("admin")
              .email("admin@example.com")
              .passwordHash("admin123")
              .rol(rolAdmin)
              .build());
          logger.info("Admin user created successfully");
      }
    };
  }

    @Bean
    public CommandLineRunner testPassword(PasswordEncoder passwordEncoder) {
        return args -> {
            String hash = "$2a$10$pGz/aP9QqsoJFKTb7im8BuRMkCFzMrvm76yJRWLtYctP8ubowhxXy";

            System.out.println(
                    "PASSWORD MATCH: " +
                            passwordEncoder.matches("admin123", hash)
            );
        };
    }

}

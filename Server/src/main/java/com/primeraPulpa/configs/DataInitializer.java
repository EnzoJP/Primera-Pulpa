package com.primeraPulpa.configs;

import com.primeraPulpa.Services.RolService;
import com.primeraPulpa.Services.UsuarioService;
import com.primeraPulpa.entities.EstadoPedido;
import com.primeraPulpa.entities.Rol;
import com.primeraPulpa.entities.UnidadMedida;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.repositories.ClienteRepository;
import com.primeraPulpa.repositories.EstadoPedidoRepository;
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
                                        UnidadMedidaRepository unidadMedidaRepository,
                                        ClienteRepository clienteRepository,
                                        EstadoPedidoRepository estadoPedidoRepository) {

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

      // Clientes iniciales
      if (clienteRepository.count() == 0) {
          com.primeraPulpa.entities.Cliente c1 = new com.primeraPulpa.entities.Cliente();
          c1.setNombre("Panadería La Argentina");
          c1.setCuit("30-71458963-2");
          c1.setContacto("+54 9 11 4321-8765");
          c1.setEliminado(false);
          clienteRepository.save(c1);

          com.primeraPulpa.entities.Cliente c2 = new com.primeraPulpa.entities.Cliente();
          c2.setNombre("Distribuidora El Sol");
          c2.setCuit("30-68954123-8");
          c2.setContacto("compras@elsol.com.ar");
          c2.setEliminado(false);
          clienteRepository.save(c2);
          logger.info("Clientes iniciales creados");
      }

      // Estados de pedido iniciales
      if (estadoPedidoRepository.findByDescripcionIgnoreCase("PENDIENTE").isEmpty()) {
          EstadoPedido ep = new EstadoPedido();
          ep.setDescripcion("PENDIENTE");
          ep.setEliminado(false);
          estadoPedidoRepository.save(ep);
          logger.info("Estado PENDIENTE creado");
      }
      if (estadoPedidoRepository.findByDescripcionIgnoreCase("PREPARADO").isEmpty()) {
          EstadoPedido ep = new EstadoPedido();
          ep.setDescripcion("PREPARADO");
          ep.setEliminado(false);
          estadoPedidoRepository.save(ep);
          logger.info("Estado PREPARADO creado");
      }
      if (estadoPedidoRepository.findByDescripcionIgnoreCase("ENTREGADO").isEmpty()) {
          EstadoPedido ep = new EstadoPedido();
          ep.setDescripcion("ENTREGADO");
          ep.setEliminado(false);
          estadoPedidoRepository.save(ep);
          logger.info("Estado ENTREGADO creado");
      }
      if (estadoPedidoRepository.findByDescripcionIgnoreCase("CANCELADO").isEmpty()) {
          EstadoPedido ep = new EstadoPedido();
          ep.setDescripcion("CANCELADO");
          ep.setEliminado(false);
          estadoPedidoRepository.save(ep);
          logger.info("Estado CANCELADO creado");
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

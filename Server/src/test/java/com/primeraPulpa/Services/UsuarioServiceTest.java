package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.entities.Rol;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.BaseRepository;
import com.primeraPulpa.repositories.PedidoRepository;
import com.primeraPulpa.repositories.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UsuarioServiceTest extends AbstractBaseServiceTest<Usuario, Long> {

    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Override
    protected BaseRepository<Usuario, Long> crearRepositoryMock() {
        return mock(UsuarioRepository.class);
    }

    @Override
    protected BaseService<Usuario, Long> crearService(BaseRepository<Usuario, Long> repository) {
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashSeguroMock");
        when(pedidoRepository.findByUsuarioId(any())).thenReturn(Collections.emptyList());

        return new UsuarioService(
                (UsuarioRepository) repository,
                pedidoRepository,
                passwordEncoder
        );
    }

    @Override
    protected Usuario crearEntidadValida() {
        Rol rol = Rol.builder().descripcion("EMPLEADO").build();
        return Usuario.builder()
                .nombre("Juan Pérez")
                .email("juan.perez@primera.com")
                .passwordHash("passwordSegura123")
                .rol(rol)
                .build();
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    // --- Validaciones en validar() ---

    @Test
    void alta_fallaSiNombreEsVacioONulo() {
        Usuario usuario = crearEntidadValida();
        usuario.setNombre("   ");

        assertThatThrownBy(() -> service.alta(usuario))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar el nombre del usuario");
    }

    @Test
    void alta_fallaSiEmailEsInvalido() {
        Usuario usuario = crearEntidadValida();
        usuario.setEmail("email_invalido_sin_formato");

        assertThatThrownBy(() -> service.alta(usuario))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar un email válido");
    }

    @Test
    void alta_fallaSiRolEsNulo() {
        Usuario usuario = crearEntidadValida();
        usuario.setRol(null);

        assertThatThrownBy(() -> service.alta(usuario))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe asignar un rol al usuario");
    }

    // --- Reglas de preAlta() ---

    @Test
    void alta_fallaSiYaExisteUsuarioConEseEmail() {
        Usuario usuario = crearEntidadValida();
        UsuarioRepository repo = (UsuarioRepository) repository;

        when(repo.findByEmail(usuario.getEmail())).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> service.alta(usuario))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Ya existe un usuario registrado con ese email");
    }

    @Test
    void alta_encriptaPasswordAntesDeGuardar() throws ErrorServiceException {
        Usuario usuario = crearEntidadValida();
        UsuarioRepository repo = (UsuarioRepository) repository;

        when(repo.findByEmail(usuario.getEmail())).thenReturn(Optional.empty());
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario guardado = service.alta(usuario);

        verify(passwordEncoder, atLeastOnce()).encode("passwordSegura123");
        assertThat(guardado.getPasswordHash()).isEqualTo("$2a$10$hashSeguroMock");
    }

    // --- Reglas de preBaja() ---

    @Test
    void bajaLogica_fallaSiIntentaDesactivarSuPropiaCuenta() {
        Long id = idDeEjemplo();
        Usuario usuarioLogueado = crearEntidadValida();
        usuarioLogueado.setId(id);
        usuarioLogueado.setEmail("admin@primera.com");

        when(repository.findById(id)).thenReturn(Optional.of(usuarioLogueado));

        // Simula la autenticación en el SecurityContext
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin@primera.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> service.bajaLogica(id))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("No podés desactivar tu propia cuenta.");
    }

    @Test
    void bajaLogica_fallaSiEsElUltimoAdministradorActivo() {
        Long id = idDeEjemplo();
        Rol rolAdmin = Rol.builder().descripcion("ADMIN").build();

        Usuario admin = crearEntidadValida();
        admin.setId(id);
        admin.setRol(rolAdmin);
        admin.setEliminado(false);

        when(repository.findById(id)).thenReturn(Optional.of(admin));
        when(repository.findAll()).thenReturn(List.of(admin)); // Solo 1 admin en el sistema

        assertThatThrownBy(() -> service.bajaLogica(id))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("No se puede desactivar al último administrador activo.");
    }

    @Test
    void bajaLogica_fallaSiElUsuarioTienePedidosAsociados() {
        Long id = idDeEjemplo();
        Usuario usuario = crearEntidadValida();
        usuario.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(usuario));
        when(pedidoRepository.findByUsuarioId(id)).thenReturn(List.of(new Pedido()));

        assertThatThrownBy(() -> service.bajaLogica(id))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("No se puede desactivar un usuario que tenga pedidos asociados.");
    }

    // --- Métodos de modificación, password y reactivación ---

    @Test
    void modificar_actualizaDatosPreservandoElPasswordHashPrevio() throws ErrorServiceException {
        Long id = idDeEjemplo();
        Usuario existente = crearEntidadValida();
        existente.setId(id);
        existente.setPasswordHash("$2a$10$hashExistenteOriginal");

        Rol nuevoRol = Rol.builder().descripcion("ADMIN").build();
        Usuario nuevosDatos = Usuario.builder()
                .nombre("Juan Modificado")
                .email("juan.nuevo@primera.com")
                .rol(nuevoRol)
                .passwordHash("intentoPisarPassword")
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Usuario> resultado = service.modificar(id, nuevosDatos);

        assertThat(resultado).isPresent();
        Usuario actualizado = resultado.get();
        assertThat(actualizado.getNombre()).isEqualTo("Juan Modificado");
        assertThat(actualizado.getEmail()).isEqualTo("juan.nuevo@primera.com");
        assertThat(actualizado.getRol().getDescripcion()).isEqualTo("ADMIN");
        assertThat(actualizado.getPasswordHash()).isEqualTo("$2a$10$hashExistenteOriginal");
    }

    @Test
    void resetPassword_actualizaHashCorrectamente() throws ErrorServiceException {
        Long id = idDeEjemplo();
        Usuario usuario = crearEntidadValida();
        usuario.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("claveNueva456")).thenReturn("$2a$10$hashClaveNueva");

        UsuarioService usuarioService = (UsuarioService) service;
        usuarioService.resetPassword(id, "claveNueva456");

        assertThat(usuario.getPasswordHash()).isEqualTo("$2a$10$hashClaveNueva");
        verify(repository, times(1)).save(usuario);
    }

    @Test
    void resetPassword_fallaSiContraseniaEsVacia() {
        UsuarioService usuarioService = (UsuarioService) service;

        assertThatThrownBy(() -> usuarioService.resetPassword(1L, "   "))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("La nueva contraseña no puede estar vacía");
    }

    @Test
    void reactivar_cambiaEliminadoAFalse() throws ErrorServiceException {
        Long id = idDeEjemplo();
        Usuario usuarioEliminado = crearEntidadValida();
        usuarioEliminado.setId(id);
        usuarioEliminado.setEliminado(true);

        when(repository.findById(id)).thenReturn(Optional.of(usuarioEliminado));

        UsuarioService usuarioService = (UsuarioService) service;
        boolean reactivado = usuarioService.reactivar(id);

        assertThat(reactivado).isTrue();
        assertThat(usuarioEliminado.getEliminado()).isFalse();
        verify(repository, times(1)).save(usuarioEliminado);
    }
}
package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService extends BaseService<Usuario, Long> {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository repository) {
        super(repository);
        this.usuarioRepository = repository;
    }

    @Override
    protected void validar(Usuario usuario) throws ErrorServiceException {
        if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar el nombre del usuario");
        }
        if (usuario.getEmail() == null || !usuario.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ErrorServiceException("Debe indicar un email válido");
        }
        if (usuario.getRol() == null) {
            throw new ErrorServiceException("Debe asignar un rol al usuario (ADMIN o EMPLEADO)");
        }
    }

    @Override
    protected void preAlta(Usuario usuario) throws ErrorServiceException {
        // HU-02: no permitir emails duplicados
        Optional<Usuario> existente = usuarioRepository.findByEmail(usuario.getEmail());
        if (existente.isPresent()) {
            throw new ErrorServiceException("Ya existe un usuario registrado con ese email");
        }
    }

    // HU-01: login por email y contraseña
    public Usuario login(String email, String password) throws ErrorServiceException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(u -> !Boolean.TRUE.equals(u.getEliminado()))
                .orElseThrow(() -> new ErrorServiceException("Usuario o contraseña incorrectos"));

        // NOTA: acá falta el hashing real (ej. BCrypt). Se deja comparación directa
        // como placeholder hasta que definamos la librería de seguridad a usar.
        if (!usuario.getPasswordHash().equals(password)) {
            throw new ErrorServiceException("Usuario o contraseña incorrectos");
        }
        return usuario;
    }
}

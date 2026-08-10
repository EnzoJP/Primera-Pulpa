package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService extends BaseService<Usuario, Long> {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        super(repository);
        this.usuarioRepository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void validar(Usuario usuario) throws ErrorServiceException {
        if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar el nombre del usuario");
        }

        if (usuario.getEmail() == null ||
                !usuario.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ErrorServiceException("Debe indicar un email válido");
        }

        if (usuario.getRol() == null) {
            throw new ErrorServiceException(
                    "Debe asignar un rol al usuario (ADMIN o EMPLEADO)"
            );
        }
    }

    @Override
    protected void preAlta(Usuario usuario) throws ErrorServiceException {
        Optional<Usuario> existente =
                usuarioRepository.findByEmail(usuario.getEmail());

        if (existente.isPresent()) {
            throw new ErrorServiceException(
                    "Ya existe un usuario registrado con ese email"
            );
        }
        usuario.setPasswordHash(
                passwordEncoder.encode(usuario.getPasswordHash())
        );
    }

    // en el alta:
    /*usuario.setPasswordHash(
            passwordEncoder.encode(usuario.getPasswordHash())
            );*/
}

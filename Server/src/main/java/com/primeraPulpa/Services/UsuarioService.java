package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.PedidoRepository;
import com.primeraPulpa.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService extends BaseService<Usuario, Long> {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PedidoRepository pedidoRepository, PasswordEncoder passwordEncoder) {
        super(repository);
        this.usuarioRepository = repository;
        this.pedidoRepository = pedidoRepository;
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

    @Override
    protected void preBaja(Long id) throws ErrorServiceException {
        List<Pedido> pedidos = pedidoRepository.findByUsuarioId(id);
        if (!pedidos.isEmpty()) {
            throw new ErrorServiceException("No se puede desactivar un usuario que tenga pedidos asociados.");
        }
    }

    @Override
    @Transactional
    public Optional<Usuario> modificar(Long id, Usuario entidadNueva) throws ErrorServiceException {
        validar(entidadNueva);
        preModificacion(entidadNueva);
        return usuarioRepository.findById(id).map(entidadExistente -> {
            entidadExistente.setNombre(entidadNueva.getNombre());
            entidadExistente.setEmail(entidadNueva.getEmail());
            entidadExistente.setRol(entidadNueva.getRol());
            // No tocamos passwordHash aquí para preservarlo
            return usuarioRepository.save(entidadExistente);
        });
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) throws ErrorServiceException {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new ErrorServiceException("La nueva contraseña no puede estar vacía");
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            throw new ErrorServiceException("El usuario no existe");
        }
        Usuario usuario = usuarioOpt.get();
        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional
    public boolean reactivar(Long id) throws ErrorServiceException {
        try {
            return usuarioRepository.findById(id).map(usuario -> {
                usuario.setEliminado(false);
                usuarioRepository.save(usuario);
                return true;
            }).orElse(false);
        } catch (Exception e) {
            throw new ErrorServiceException("Error al reactivar el usuario");
        }
    }

    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }
}

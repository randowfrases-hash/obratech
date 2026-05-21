package com.obratech.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final PerfilRepository perfilRepository;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder passwordEncoder,
                          PerfilRepository perfilRepository) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.perfilRepository = perfilRepository;
    }

    public List<Usuario> findAll() { return repo.findAll(); }

    public Optional<Usuario> findById(String id) { 
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        return repo.findById(safeId); 
    }

    public Optional<Usuario> findByUsername(String username) { return repo.findByUsername(username); }

    /**
     * Registra un nuevo usuario validando campos, rol y encriptando la contrasea.
     * Crea automticamente un documento en la coleccin `perfiles` con el rol asignado.
     */
    public Usuario register(Usuario u) {
        if (u.getUsername() == null || u.getUsername().isBlank())
            throw new IllegalArgumentException("El correo electrnico no puede estar vaco.");
        if (!u.getUsername().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Debes ingresar un correo electrnico vlido.");
        if (u.getPassword() == null || u.getPassword().isBlank())
            throw new IllegalArgumentException("La contrasea no puede estar vaca.");
        if (u.getPassword().length() < 8)
            throw new IllegalArgumentException("La contrasea debe tener al menos 8 caracteres.");
        if (repo.findByUsername(u.getUsername()).isPresent())
            throw new IllegalArgumentException("El correo electrnico ya est registrado.");

        // Normalizar roles
        java.util.Set<String> rolesNormalizados = new java.util.HashSet<>();
        if (u.getRoles() != null) {
            for (String r : u.getRoles()) {
                rolesNormalizados.add(mapearRol(r));
            }
        }
        if (rolesNormalizados.isEmpty()) {
            rolesNormalizados.add("ROLE_USER");
        }
        u.setRoles(rolesNormalizados);

        // Encriptar contrasea
        u.setPassword(passwordEncoder.encode(u.getPassword()));
        u.setCreado(LocalDateTime.now());
        u.setActivo(true); // Se activa automáticamente en desarrollo para permitir login inmediato

        Usuario savedUser = repo.save(u);

        // Crear perfil en la coleccin `perfiles` si tiene algn rol de negocio
        boolean esRolDeNegocio = savedUser.getRoles().contains("ROLE_WORKER") || 
                                 savedUser.getRoles().contains("ROLE_CONTRACTOR") || 
                                 savedUser.getRoles().contains("ROLE_CLIENT");

        if (esRolDeNegocio && perfilRepository.findByUsername(savedUser.getUsername()).isEmpty()) {
            Perfil p = new Perfil();
            p.setUsername(savedUser.getUsername());
            p.setEmail(savedUser.getUsername());
            p.setRoles(savedUser.getRoles());
            p.setActivo(true); // Activo por defecto para permitir login inmediato
            p.setVerificado(false);
            p.setCreado(LocalDateTime.now());

            if (savedUser.getRoles().contains("ROLE_WORKER")) {
                p.setDisponibilidad(true);
            }

            if (savedUser.getRoles().contains("ROLE_CONTRACTOR")) {
                p.setCalificacionPromedio(0.0);
            }

            if (savedUser.getRoles().contains("ROLE_CLIENT")) {
                // Cliente no tiene default fields numricos/booleanos por ahora
            }

            perfilRepository.save(p);
        }

        return savedUser;
    }

    /**
     * Activa o desactiva una cuenta.
     */
    public void toggleActivo(String id) {
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        repo.findById(safeId).ifPresent(u -> {
            u.setActivo(!u.isActivo());
            @SuppressWarnings("null")
            Usuario safeU = u;
            repo.save(safeU);
        });
    }

    public void deleteById(String id) { 
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        repo.deleteById(safeId); 
    }

    //  Mapea el valor del select del registro 
    private String mapearRol(String roleSolicitado) {
        if (roleSolicitado == null) return "ROLE_USER";
        return switch (roleSolicitado.toLowerCase()) {
            case "contratista" -> "ROLE_CONTRACTOR";
            case "cliente"     -> "ROLE_CLIENT";
            case "trabajador"  -> "ROLE_WORKER";
            // Eliminado case "admin" por seguridad: un usuario normal nunca debe registrarse como admin
            default            -> "ROLE_USER";
        };
    }
}

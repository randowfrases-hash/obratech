package com.obratech.security;

import com.obratech.entity.Usuario;
import com.obratech.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio para controlar intentos fallidos de inicio de sesin.
 * Bloquea la cuenta temporalmente despus de MAX_INTENTOS fallos.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepository usuarioRepository;

    public LoginAttemptService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Llamado cuando el login falla: incrementa el contador y bloquea si supera el lmite. */
    public void registrarFallo(String username) {
        Optional<Usuario> opt = usuarioRepository.findByUsername(username);
        if (opt.isEmpty()) return;

        Usuario u = opt.get();
        int intentos = u.getIntentosFallidos() + 1;
        u.setIntentosFallidos(intentos);

        if (intentos >= MAX_INTENTOS) {
            u.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
            u.setIntentosFallidos(0); // reset para el prximo ciclo
        }
        usuarioRepository.save(u);
    }

    /** Llamado cuando el login es exitoso: limpia el contador. */
    public void registrarExito(String username) {
        Optional<Usuario> opt = usuarioRepository.findByUsername(username);
        if (opt.isEmpty()) return;

        Usuario u = opt.get();
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        u.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(u);
    }

    /** Retorna true si la cuenta est bloqueada en este momento. */
    public boolean estaBloqueado(String username) {
        return usuarioRepository.findByUsername(username)
                .map(u -> u.getBloqueadoHasta() != null && LocalDateTime.now().isBefore(u.getBloqueadoHasta()))
                .orElse(false);
    }
}

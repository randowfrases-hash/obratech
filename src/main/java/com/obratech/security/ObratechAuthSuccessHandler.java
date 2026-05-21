package com.obratech.security;

import com.obratech.entity.Usuario;
import com.obratech.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class ObratechAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;
    private final LoginAttemptService loginAttemptService;

    public ObratechAuthSuccessHandler(UsuarioRepository usuarioRepository,
                                      LoginAttemptService loginAttemptService) {
        this.usuarioRepository = usuarioRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String username = authentication.getName();

        //  Limpiar intentos fallidos y registrar ltimo acceso 
        loginAttemptService.registrarExito(username);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            HttpSession session = request.getSession(true);

            // Guardar usuario en sesin (los controladores lo usan)
            session.setAttribute("usuario", usuario);

            //  Atributos tiles de sesin para las vistas 
            session.setAttribute("sessionCreatedAt", System.currentTimeMillis());
            session.setAttribute("userRoles", usuario.getRoles());
            session.setAttribute("userRole", usuario.getRole()); // fallback por compatibilidad

            System.out.println(" Login exitoso: " + usuario.getUsername() + " " + usuario.getRoles());
        }

        response.sendRedirect("/desboard");
    }
}

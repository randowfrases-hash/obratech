package com.obratech.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.obratech.entity.Usuario;
import com.obratech.repository.UsuarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        // Obtener el nombre del usuario autenticado
        String username = authentication.getName();
        
        // Buscar el usuario en la BD
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        
        // Guardar en la sesion
        HttpSession session = request.getSession();
        session.setAttribute("usuario", usuario);
        if (usuario != null) {
            session.setAttribute("userRole", usuario.getRole());
            session.setAttribute("userRoles", usuario.getRoles());
            session.setAttribute("sessionCreatedAt", System.currentTimeMillis());
        }
        
        // Redireccionar segun el rol determinista
        String role = usuario != null ? usuario.getRole() : "ROLE_USER";

        switch (role) {
            case "ROLE_WORKER" -> response.sendRedirect("/desboard-trabajador");
            case "ROLE_ADMIN" -> response.sendRedirect("/admin/desboard");
            case "ROLE_CLIENT" -> response.sendRedirect("/desboard-cliente");
            case "ROLE_CONTRACTOR" -> response.sendRedirect("/desboard-contratista");
            default -> response.sendRedirect("/");
        }
    }
}

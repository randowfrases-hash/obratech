package com.obratech.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manejador de fallos de autenticacin.
 * Registra el intento fallido y redirige con mensaje apropiado.
 */
@Component
public class ObratechAuthFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public ObratechAuthFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        if (username != null && !username.isBlank()) {
            loginAttemptService.registrarFallo(username);

            // Si la cuenta qued bloqueada por esta falla, indicarlo
            if (loginAttemptService.estaBloqueado(username)) {
                response.sendRedirect("/login?blocked=true");
                return;
            }
        }

        response.sendRedirect("/login?error=true");
    }
}

package com.obratech.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.security.JwtService;

/**
 * Endpoint REST para autenticación con JWT.
 *
 * POST /api/auth/login
 *   Body:     { "username": "...", "password": "..." }
 *   Response: { "token": "eyJhb...", "tipo": "Bearer", "username": "..." }
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthApiController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** DTO del body de login. */
    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Normalizar igual que UserDetailsServiceImpl para evitar inconsistencias
            String username = loginRequest.username().trim().toLowerCase();

            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.password())
            );

            String token = jwtService.generarToken(auth.getName(), auth.getAuthorities());

            return ResponseEntity.ok(Map.of(
                    "token",    token,
                    "tipo",     "Bearer",
                    "username", auth.getName()
            ));

        } catch (BadCredentialsException e) {
            // No revelar si falló el username o la contraseña (seguridad)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }
}

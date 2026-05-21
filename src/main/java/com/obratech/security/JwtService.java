package com.obratech.security;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Genera, valida y extrae claims de tokens JWT (HMAC-SHA256).
 * La clave y expiración se leen desde application.properties.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    // Duración del token en ms (default: 86400000 = 24 horas)
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /** Genera un token JWT firmado con username y roles del usuario. */
    public String generarToken(String username,
                               Collection<? extends GrantedAuthority> authorities) {
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Date ahora  = new Date();
        Date expira = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Valida firma y expiración del token.
     * Retorna false ante cualquier error, sin exponer detalles al cliente.
     */
    public boolean esTokenValido(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Extrae el username (claim "sub") del token. */
    public String extraerUsername(String token) {
        return getClaims(token).getSubject();
    }

    /** Extrae la lista de roles del claim "roles". */
        @SuppressWarnings({"unchecked", "rawtypes"})
    public List<String> extraerRoles(String token) {
        Object rolesClaim = getClaims(token).get("roles");
        if (rolesClaim instanceof List<?> lista) {
            return lista.stream()
                    .filter(r -> r instanceof String)
                    .map(r -> (String) r)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // La clave debe tener mínimo 32 bytes para HS256
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}

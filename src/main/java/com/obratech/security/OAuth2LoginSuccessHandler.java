package com.obratech.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * ══════════════════════════════════════════════════════════════════
 *  OAuth2LoginSuccessHandler — Login exitoso con Google (OAuth2/OIDC)
 * ══════════════════════════════════════════════════════════════════
 *
 * Se ejecuta AUTOMATICAMENTE cuando el usuario completa el login con Google.
 *
 * Flujo:
 *   1. Extrae email y nombre del token de Google (OIDC o OAuth2 genérico).
 *   2. Busca el Usuario en MongoDB por email.
 *      - Si existe → usa el existente (mantiene su rol actual).
 *      - Si es nuevo → crea un Usuario con ROLE_GUEST por defecto.
 *   3. Sincroniza los Roles de la base de datos con el SecurityContext de Spring.
 *   4. Guarda el Usuario en la sesión HTTP (compatible con el resto del sistema).
 *   5. Redirige al dashboard según el rol del usuario.
 *
 * Manejo de errores:
 *   - Se captura cualquier Exception (no solo IOException) para evitar que
 *     errores de tipo cast o NPE queden silenciados.
 *   - Si falla cualquier paso, se redirige a /login?error=google.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PerfilRepository perfilRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // ── Extraer datos del principal de Google ──────────────────────────────
        // Google puede devolver OidcUser (cuando scope incluye openid) o
        // un OAuth2User genérico. Manejamos ambos casos.
        String email;
        String nombre;
        String apellido;

        try {
            System.out.println("[DEBUG-OAuth2] Iniciando proceso de éxito de autenticación...");
            Object principal = authentication.getPrincipal();
            System.out.println("[DEBUG-OAuth2] Principal detectado: " + principal.getClass().getName());

            if (principal instanceof OidcUser oidcUser) {
                // Caso preferido: OIDC con token de identidad completo
                email    = oidcUser.getEmail();
                nombre   = oidcUser.getGivenName();
                apellido = oidcUser.getFamilyName();

            } else if (principal instanceof OAuth2User oauth2User) {
                // Fallback: OAuth2 sin OIDC (ej. Google sin scope openid)
                email    = oauth2User.getAttribute("email");
                nombre   = oauth2User.getAttribute("given_name");
                apellido = oauth2User.getAttribute("family_name");

            } else {
                // Principal de tipo inesperado — no podemos continuar
                System.err.println("[OAuth2] Principal de tipo desconocido: "
                        + principal.getClass().getName());
                response.sendRedirect("/login?error=google");
                return;
            }

            // ── Validar que el email no sea nulo ───────────────────────────────
            // Sin email no podemos identificar al usuario en MongoDB
            if (email == null || email.isBlank()) {
                System.err.println("[DEBUG-OAuth2] ERROR: Google no proporcionó un email válido.");
                response.sendRedirect("/login?error=google");
                return;
            }
            System.out.println("[DEBUG-OAuth2] Email extraído: " + email);

            // ── Normalizar valores ─────────────────────────────────────────────
            final String finalEmail    = email.trim().toLowerCase();
            final String finalNombre   = (nombre   != null && !nombre.isBlank())   ? nombre   : finalEmail;
            final String finalApellido = (apellido != null && !apellido.isBlank()) ? apellido : "";

            // ── Sincronizar Usuario y Perfil ──────────────────────────────────
            System.out.println("[DEBUG-OAuth2] Buscando usuario en base de datos...");
            Usuario usuario = usuarioRepository.findByUsername(finalEmail).orElse(null);
            boolean esNuevo = false;

            if (usuario == null) {
                System.out.println("[DEBUG-OAuth2] Usuario no encontrado. Creando nuevo perfil GUEST...");
                esNuevo = true;
                usuario = new Usuario();
                usuario.setUsername(finalEmail);
                usuario.setPassword(""); 
                usuario.setRoles(new java.util.HashSet<>(java.util.Collections.singleton("ROLE_GUEST")));
                usuario.setActivo(false);
                usuario.setVerificado(false);
                usuario.setCreado(LocalDateTime.now());
                usuario = usuarioRepository.save(usuario);
                System.out.println("[OAuth2] Creado nuevo usuario GUEST: " + finalEmail);
            } else {
                System.out.println("[DEBUG-OAuth2] Usuario existente encontrado: " + usuario.getUsername() + " con roles: " + usuario.getRoles());
                // Si el usuario existe, verificamos si tiene perfil
                boolean tienePerfil = perfilRepository.findByUsername(finalEmail).isPresent();
                boolean tieneRolesNegocio = usuario.getRoles().stream()
                        .anyMatch(r -> r.equals("ROLE_CLIENT") || r.equals("ROLE_CONTRACTOR") || r.equals("ROLE_WORKER"));
                
                if (!tienePerfil || !tieneRolesNegocio) {
                    esNuevo = true;
                    System.out.println("[OAuth2] Usuario existente sin perfil o roles de negocio: " + finalEmail);
                }
            }

            usuario.setUltimoAcceso(LocalDateTime.now());
            usuario = usuarioRepository.save(usuario);

            // ── Preparar Sesión (SIN INVALIDAR) ───────────────────────────────
            HttpSession session = request.getSession(true);
            session.setAttribute("usuario", usuario);
            session.setAttribute("userRole", usuario.getRole());
            session.setAttribute("userRoles", usuario.getRoles());
            session.setAttribute("oauth2_nombre", finalNombre);
            session.setAttribute("oauth2_apellido", finalApellido);
            
            // ── Sincronizar SecurityContext con los roles de la DB ────────────────
            // Esto es CRUCIAL para evitar errores 403 al redirigir al dashboard.
            // Spring Security por defecto solo tiene los scopes de Google, no nuestros roles.
            try {
                java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = 
                    usuario.getRoles().stream()
                        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                        .collect(java.util.stream.Collectors.toList());

                org.springframework.security.authentication.UsernamePasswordAuthenticationToken newAuth = 
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        authentication.getPrincipal(), 
                        null, 
                        authorities
                    );
                
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);
                
                // Forzar persistencia en sesión para Spring Security 6
                session.setAttribute("SPRING_SECURITY_CONTEXT", org.springframework.security.core.context.SecurityContextHolder.getContext());
                
                System.out.println("[DEBUG-OAuth2] SecurityContext sincronizado con roles: " + usuario.getRoles());
            } catch (Exception authEx) {
                System.err.println("[DEBUG-OAuth2] Error sincronizando SecurityContext: " + authEx.getMessage());
            }

            String targetUrl = esNuevo ? "/completar-registro-oauth2" : "/desboard";
            System.out.println("[DEBUG-OAuth2] Finalizando. Redirigiendo a: " + targetUrl);
            
            response.sendRedirect(targetUrl);
            return;

        } catch (Exception e) {
            // ── Captura general: cualquier error inesperado ────────────────────
            // Se usa Exception (no solo IOException) para capturar también
            // ClassCastException, NullPointerException, MongoException, etc.
            System.err.println("[OAuth2] Error durante el login con Google: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());

            // Solo redirigir si la respuesta no ha sido comprometida ya
            if (!response.isCommitted()) {
                response.sendRedirect("/login?error=google");
            }
        }
    }
}

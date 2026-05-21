package com.obratech.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.obratech.security.CustomAccessDeniedHandler;
import com.obratech.security.CustomAuthenticationSuccessHandler;
import com.obratech.security.JwtAuthFilter;
import com.obratech.security.OAuth2LoginSuccessHandler;
import com.obratech.security.UserDetailsServiceImpl;

/**
 * ══════════════════════════════════════════════════════════════════
 *  SecurityConfig — Configuración central de seguridad de ObraTech
 * ══════════════════════════════════════════════════════════════════
 *
 * Implementa una estrategia de seguridad HÍBRIDA:
 *
 *   [A] Rutas WEB (Thymeleaf) → usan SESIÓN HTTP (como antes)
 *       - Login por formulario: /login
 *       - Login con Google: OAuth2
 *       - Protegidas por rol: /desboard, /admin/**, /perfil-*, etc.
 *
 *   [B] Rutas API REST → usan JWT (stateless)
 *       - Endpoint de login: POST /api/auth/login → retorna token
 *       - Rutas protegidas: /api/** con header "Authorization: Bearer <token>"
 *       - No se crea ni consulta sesión HTTP para la API
 *
 * El filtro JwtAuthFilter se ejecuta ANTES del UsernamePasswordAuthenticationFilter
 * de Spring para que pueda inyectar la autenticación JWT en el SecurityContext.
 */
@Configuration
public class SecurityConfig {

    // ── Handlers del flujo web (formulario + Google) ──────────────────────────
    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;

    // ── Filtro JWT para la API REST ───────────────────────────────────────────
    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    // ─────────────────────────────────────────────────────────────────────────
    // BEANS DE CONFIGURACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Encoder de contraseñas usando BCrypt.
     * Se usa en el registro de usuarios y en la autenticación por formulario.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager expuesto como Bean.
     * Necesario para que AuthApiController pueda autenticar credenciales
     * programáticamente (sin el formulario HTML).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Proveedor de autenticación DAO.
     * Conecta el UserDetailsService (MongoDB) con el PasswordEncoder (BCrypt).
     */
    @Bean
    @SuppressWarnings("deprecation")
    public DaoAuthenticationProvider authenticationProvider(UserDetailsServiceImpl userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CADENA DE FILTROS DE SEGURIDAD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Define la cadena principal de filtros de seguridad HTTP.
     *
     * Orden de configuración importante:
     *   1. authenticationProvider → quién autentica
     *   2. csrf → deshabilitado (API REST no necesita CSRF; la web usa SameSite=Strict)
     *   3. authorizeHttpRequests → reglas de acceso por ruta
     *   4. addFilterBefore → registrar JwtAuthFilter antes del filtro estándar de usuario
     *   5. formLogin → configuración del login web con formulario
     *   6. oauth2Login → configuración del login con Google
     *   7. logout → configuración del logout
     *   8. sessionManagement → máximo 1 sesión activa por usuario
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
            // ── Proveedor de autenticación (MongoDB + BCrypt) ────────────────
            .authenticationProvider(authenticationProvider)

            // ── CSRF deshabilitado ───────────────────────────────────────────
            // La API REST usa JWT (stateless, sin estado de sesión vulnerable).
            // La web usa cookies con SameSite=Strict (ver application.properties).
            .csrf(csrf -> csrf.disable())

            // ── Reglas de autorización por ruta ─────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // ── Rutas PÚBLICAS (no requieren autenticación) ──────────────
                .requestMatchers(
                        "/", "/login", "/registro",
                        "/css/**", "/js/**", "/img/**", "/styles/**", "/error",
                        "/oauth2/**", "/login/oauth2/**", "/completar-registro-oauth2"
                ).permitAll()

                // ── Dashboard General (Punto de entrada para todos los roles) ──
                .requestMatchers("/desboard", "/seleccionar-perfil").authenticated()

                // ── Endpoint de login JWT (público, es el que emite tokens) ──
                // Si se protegiera, nadie podría obtener el token inicial.
                .requestMatchers("/api/auth/**").permitAll()

                // ── Rutas de la REST API (requieren JWT válido) ───────────────
                // JwtAuthFilter valida el token antes de llegar aquí.
                .requestMatchers("/api/**").authenticated()

                // ── Rutas del CLIENTE ────────────────────────────────────────
                .requestMatchers("/perfil-cliente/**", "/mis-proyectos/**")
                .hasAuthority("ROLE_CLIENT")

                // ── Rutas del CONTRATISTA ────────────────────────────────────
                .requestMatchers("/desboard-contratista", "/perfil-contratista/**", "/contratista-proyectos/**")
                .hasAuthority("ROLE_CONTRACTOR")

                // ── Rutas del TRABAJADOR ─────────────────────────────────────
                .requestMatchers("/desboard-trabajador", "/perfil-laboral/**")
                .hasAuthority("ROLE_WORKER")

                // ── Rutas del ADMINISTRADOR ──────────────────────────────────
                .requestMatchers("/admin/**")
                .hasAuthority("ROLE_ADMIN")

                // ── Cualquier otra ruta requiere autenticación ───────────────
                .anyRequest().authenticated()
            )
            
            // ── Manejo de excepciones (403 Acceso Denegado) ──────────────────
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
            )

            // ── Registrar el filtro JWT ──────────────────────────────────────
            // Se ejecuta ANTES del filtro estándar de Spring para que la
            // autenticación JWT esté lista cuando Spring Security la necesite.
            // JwtAuthFilter tiene shouldNotFilter() que lo limita a /api/**
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // ── Login con formulario HTML (flujo web existente) ──────────────
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .failureUrl("/login?error=true")
                .successHandler(authenticationSuccessHandler)
                .permitAll()
            )

            // ── Login con Google OAuth2 (flujo web existente) ────────────────
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(oauth2LoginSuccessHandler)
            )

            // ── Logout ───────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("OBRATECH_SESSION")
                .permitAll()
            )

            // ── Gestión de sesiones web ──────────────────────────────────────
            // Solo para las rutas web; la API es stateless (sin sesión).
            // SessionCreationPolicy.IF_REQUIRED: crea sesión solo si la necesita
            // (rutas web la necesitan, la API JWT no).
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            );

        return http.build();
    }
}

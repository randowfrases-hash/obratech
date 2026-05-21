package com.obratech.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoValidacion;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private ProyectoRepository      proyectoRepository;
    @Autowired private PerfilRepository        perfilRepository;
    @Autowired private CalificacionRepository  calificacionRepository;
    @Autowired private UsuarioRepository       usuarioRepository;
    @Autowired private com.obratech.service.SeedService seedService;

    // ─────────────────────────────────────────────────────────────────────────
    // DASHBOARD PRINCIPAL
    // ─────────────────────────────────────────────────────────────────────────
 
    @GetMapping({"", "/", "/desboard"})
    public String dashboardAdmin(HttpSession session, Model model) {

        // Obtener el usuario del contexto (sesión o Spring Security)
        Usuario usuario = resolverUsuarioAdmin(session);
        if (usuario == null) return "redirect:/login";

        // ── Proyectos pendientes de validación ─────────────────────────────────
        List<Proyecto> proyectosPendientes =
                proyectoRepository.findByEstadoValidacion(EstadoValidacion.PENDIENTE);
        List<Proyecto> proyectosAprobados =
                proyectoRepository.findByEstadoValidacion(EstadoValidacion.APROBADO);
        List<Proyecto> proyectosRechazados =
                proyectoRepository.findByEstadoValidacion(EstadoValidacion.RECHAZADO);

        // ── Contratistas ───────────────────────────────────────────────────────
        // findByRolesAndActivoTrue usa @Query en MongoDB: { roles: "ROLE_CONTRACTOR", activo: true }
        List<Perfil> contratistas       = perfilRepository.findByRolesAndActivoTrue("ROLE_CONTRACTOR");
        // findByRolesAndVerificadoFalse: { roles: "ROLE_CONTRACTOR", $or: [{verificado:false},{verificado:null}] }
        List<Perfil> contratistasNoVer  = perfilRepository.findByRolesAndVerificadoFalse("ROLE_CONTRACTOR");

        // ── Clientes ───────────────────────────────────────────────────────────
        List<Perfil> clientes           = perfilRepository.findByRolesAndActivoTrue("ROLE_CLIENT");
        List<Perfil> clientesNoVer      = perfilRepository.findByRolesAndVerificadoFalse("ROLE_CLIENT");

        // ── Trabajadores ───────────────────────────────────────────────────────
        // findByRoles: { roles: "ROLE_WORKER" } — todos, activos o no
        List<Perfil> trabajadores       = perfilRepository.findByRoles("ROLE_WORKER");
        List<Perfil> trabajadoresNoVer  = perfilRepository.findByRolesAndVerificadoFalse("ROLE_WORKER");

        // ── Inactivos (Suspended/Disabled) ───────────────────────────────────
        List<Perfil> contratistasInactivos = perfilRepository.findByRolesAndActivoFalse("ROLE_CONTRACTOR");
        List<Perfil> clientesInactivos     = perfilRepository.findByRolesAndActivoFalse("ROLE_CLIENT");
        List<Perfil> trabajadoresInactivos = perfilRepository.findByRolesAndActivoFalse("ROLE_WORKER");

        // ── Enviar datos al template ───────────────────────────────────────────
        model.addAttribute("usuario",                  usuario);
        model.addAttribute("proyectosPendientes",      proyectosPendientes);
        model.addAttribute("proyectosAprobados",       proyectosAprobados);
        model.addAttribute("proyectosRechazados",      proyectosRechazados);
        model.addAttribute("totalProyectos",           proyectosPendientes.size());
        model.addAttribute("totalProyectosAprobados",  proyectosAprobados.size());
        model.addAttribute("totalProyectosRechazados", proyectosRechazados.size());
        model.addAttribute("contratistas",             contratistas);
        model.addAttribute("totalContratistas",        contratistas.size());
        model.addAttribute("clientes",                 clientes);
        model.addAttribute("totalClientes",            clientes.size());
        model.addAttribute("contratistasNoVerificados", contratistasNoVer);
        model.addAttribute("clientesNoVerificados",    clientesNoVer);
        model.addAttribute("trabajadoresNoVerificados", trabajadoresNoVer);
        model.addAttribute("trabajadores",             trabajadores);
        model.addAttribute("totalTrabajadores",        trabajadores.size());
        
        // Listas de inactivos
        model.addAttribute("contratistasInactivos",    contratistasInactivos);
        model.addAttribute("clientesInactivos",        clientesInactivos);
        model.addAttribute("trabajadoresInactivos",    trabajadoresInactivos);
        model.addAttribute("totalInactivos",           contratistasInactivos.size() + clientesInactivos.size() + trabajadoresInactivos.size());

        // ── Estadísticas Globales del Sistema ──────────────────────────────────
        model.addAttribute("statsTotalUsuarios",       usuarioRepository.count());
        model.addAttribute("statsTotalProyectos",      proyectoRepository.count());
        model.addAttribute("statsUsuariosPendientes",  contratistasNoVer.size() + clientesNoVer.size() + trabajadoresNoVer.size());
        model.addAttribute("statsProyectosPendientes", proyectosPendientes.size());

        return "desboard-admin";
    }

    /** Endpoint para la generación masiva de 15,000 registros de prueba. */
    @PostMapping("/seed-15k")
    public String generar15kDatos(HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/login";
        try {
            seedService.seed15kData();
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Se generaron exitosamente 15,000 registros de prueba repartidos entre todas las colecciones!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al generar los datos: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/admin/desboard";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE PROYECTOS
    // ─────────────────────────────────────────────────────────────────────────

    /** Aprueba un proyecto cambiando su EstadoValidacion a APROBADO. */
    @PostMapping("/proyectos/{id}/aprobar")
    public String aprobarProyecto(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        proyectoRepository.findById(safeId).ifPresent(p -> {
            p.setEstadoValidacion(EstadoValidacion.APROBADO);
            proyectoRepository.save(p);
        });
        return "redirect:/admin/desboard";
    }

    /** Rechaza un proyecto cambiando su EstadoValidacion a RECHAZADO. */
    @PostMapping("/proyectos/{id}/rechazar")
    public String rechazarProyecto(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId2 = id != null ? id : "";
        proyectoRepository.findById(safeId2).ifPresent(p -> {
            p.setEstadoValidacion(EstadoValidacion.RECHAZADO);
            proyectoRepository.save(p);
        });
        return "redirect:/admin/desboard";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE CONTRATISTAS
    // ─────────────────────────────────────────────────────────────────────────

    /** Activa o suspende un contratista (toggle de campo activo). */
    @PostMapping("/contratistas/{id}/toggle")
    public String toggleContratista(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId3 = id != null ? id : "";
        perfilRepository.findById(safeId3).ifPresent(p -> {
            boolean nuevoActivo = !Boolean.TRUE.equals(p.getActivo());
            p.setActivo(nuevoActivo);
            perfilRepository.save(p);
            // Sincronizar estado activo en la colección usuarios
            usuarioRepository.findByUsernameIgnoreCase(p.getUsername()).ifPresent(u -> {
                u.setActivo(nuevoActivo);
                usuarioRepository.save(u);
            });
        });
        return "redirect:/admin/desboard";
    }

    /** Marca un contratista como verificado por el admin. */
    @PostMapping("/contratistas/{id}/verificar")
    public String verificarContratista(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId4 = id != null ? id : "";
        perfilRepository.findById(safeId4).ifPresent(p -> {
            p.setVerificado(true);
            perfilRepository.save(p);
            // Sincronizar estado verificado en la colección usuarios
            usuarioRepository.findByUsernameIgnoreCase(p.getUsername()).ifPresent(u -> {
                u.setVerificado(true);
                usuarioRepository.save(u);
            });
        });
        return "redirect:/admin/desboard";
    }

    /**
     * Vista de detalles de un contratista específico.
     * Muestra sus datos de perfil, calificaciones y proyectos asignados.
     */
    @GetMapping("/contratistas/{id}")
    public String verDetallesContratista(@PathVariable String id,
                                        HttpSession session,
                                        Model model) {
        Usuario usuario = resolverUsuarioAdmin(session);
        if (usuario == null) return "redirect:/login";

        @SuppressWarnings("null")
        String safeId5 = id != null ? id : "";
        Perfil contratista = perfilRepository.findById(safeId5).orElse(null);
        if (contratista == null) return "redirect:/admin/desboard";

        model.addAttribute("contratista", contratista);
        model.addAttribute("usuario", usuario);

        // Cargar calificaciones del contratista si tiene email
        if (contratista.getEmail() != null) {
            perfilRepository.findByEmailIgnoreCase(contratista.getEmail())
                    .ifPresent(p -> model.addAttribute(
                            "calificaciones",
                            calificacionRepository.findByContratistaId(p.getId())
                    ));
        }

        // Cargar proyectos asignados a este contratista
        List<Proyecto> proyectosDelContratista =
                proyectoRepository.findByContratistaAsignadoId(id);
        model.addAttribute("proyectosDelContratista", proyectosDelContratista);

        return "admin-detalles-contratista";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE CLIENTES
    // ─────────────────────────────────────────────────────────────────────────

    /** Activa o suspende un cliente (toggle de campo activo). */
    @PostMapping("/clientes/{id}/toggle")
    public String toggleCliente(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId6 = id != null ? id : "";
        perfilRepository.findById(safeId6).ifPresent(p -> {
            boolean nuevoActivo = !Boolean.TRUE.equals(p.getActivo());
            p.setActivo(nuevoActivo);
            perfilRepository.save(p);
            // Sincronizar estado activo en la colección usuarios
            usuarioRepository.findByUsernameIgnoreCase(p.getUsername()).ifPresent(u -> {
                u.setActivo(nuevoActivo);
                usuarioRepository.save(u);
            });
        });
        return "redirect:/admin/desboard";
    }

    /** Marca un cliente como verificado por el admin. */
    @PostMapping("/clientes/{id}/verificar")
    public String verificarCliente(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId7 = id != null ? id : "";
        perfilRepository.findById(safeId7).ifPresent(p -> {
            p.setVerificado(true);
            perfilRepository.save(p);
            // Sincronizar estado verificado en la colección usuarios
            usuarioRepository.findByUsernameIgnoreCase(p.getUsername()).ifPresent(u -> {
                u.setVerificado(true);
                usuarioRepository.save(u);
            });
        });
        return "redirect:/admin/desboard";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE TRABAJADORES
    // ─────────────────────────────────────────────────────────────────────────

    /** Activa o suspende un trabajador (toggle de campo activo). */
    @PostMapping("/trabajadores/{id}/toggle")
    public String toggleTrabajador(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId8 = id != null ? id : "";
        perfilRepository.findById(safeId8).ifPresent(t -> {
            boolean nuevoActivo = !Boolean.TRUE.equals(t.getActivo());
            t.setActivo(nuevoActivo);
            perfilRepository.save(t);
            // Sincronizar estado activo en la colección usuarios
            usuarioRepository.findByUsernameIgnoreCase(t.getUsername()).ifPresent(u -> {
                u.setActivo(nuevoActivo);
                usuarioRepository.save(u);
            });
        });
        // FIXED: redirigir al dashboard del admin, no a /trabajadores (ruta inexistente para admin)
        return "redirect:/admin/desboard";
    }

    /** Marca un trabajador como verificado por el admin. */
    @PostMapping("/trabajadores/{id}/verificar")
    public String verificarTrabajador(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        perfilRepository.findById(safeId).ifPresent(p -> {
            p.setVerificado(true);
            perfilRepository.save(p);
            // Sincronizar estado verificado en la colección usuarios
            usuarioRepository.findByUsernameIgnoreCase(p.getUsername()).ifPresent(u -> {
                u.setVerificado(true);
                usuarioRepository.save(u);
            });
        });
        return "redirect:/admin/desboard";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS DE SOPORTE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resuelve el usuario administrador desde la sesión HTTP.
     * Si la sesión no tiene el usuario (ej. tras login con Google),
     * lo busca en la base de datos usando el nombre del SecurityContext.
     *
     * @return El Usuario con ROLE_ADMIN, o null si no es admin.
     */
    private Usuario resolverUsuarioAdmin(HttpSession session) {
        // Intento 1: leer desde la sesión (flujo normal)
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        // Intento 2: si la sesión no tiene usuario, buscarlo por Spring Security
        // Esto cubre el caso de OAuth2 login donde la sesión podría no tener el objeto
        if (usuario == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                usuario = usuarioRepository.findByUsernameIgnoreCase(auth.getName()).orElse(null);
                // Si lo encontramos, guardarlo en sesión para próximas requests
                if (usuario != null) {
                    session.setAttribute("usuario", usuario);
                }
            }
        }

        // Verificar que el usuario tiene rol de admin
        if (usuario == null || !usuario.getRoles().contains("ROLE_ADMIN")) {
            return null;
        }
        return usuario;
    }

    /**
     * Verifica rápidamente si la sesión pertenece a un admin.
     * Se usa en los endpoints POST donde solo necesitamos validar el rol.
     */
    private boolean isAdmin(HttpSession session) {
        return resolverUsuarioAdmin(session) != null;
    }
}

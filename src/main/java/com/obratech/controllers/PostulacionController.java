package com.obratech.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.obratech.entity.Proyecto;
import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoAsignacion;
import com.obratech.entity.enums.EstadoPostulacion;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/postulaciones")
public class PostulacionController {

    @Autowired private ProyectoRepository proyectoRepository;
    @Autowired private com.obratech.repository.PostulacionRepository postulacionRepository;
    @Autowired private PerfilRepository perfilRepository;
    @Autowired private com.obratech.repository.UsuarioRepository usuarioRepository;

    // Enviar postulacin a un proyecto - PERSISTE EN DB
    @PostMapping("/postular/{id}")
    public String postularseProyecto(
            @PathVariable String id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_WORKER") && !usuario.getRoles().contains("ROLE_CONTRACTOR"))) {
            return "redirect:/desboard";
        }

        // Verificar si el usuario está verificado por el administrador antes de postularse
        Usuario dbUsuario = usuarioRepository.findById(usuario.getId()).orElse(null);
        if (dbUsuario == null || !dbUsuario.isVerificado()) {
            session.setAttribute("error", "No puedes postularte a proyectos hasta que tu cuenta sea verificada por el administrador.");
            return "redirect:/postulaciones/" + id;
        }

        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);

        if (proyecto == null) {
            return "redirect:/postulaciones";
        }

        try {
            // Verificar si ya existe una postulación PENDIENTE
            java.util.List<com.obratech.entity.Postulacion> postulacionesExistentes = 
                postulacionRepository.findByProyectoId(id).stream()
                    .filter(p -> p.getUsuario() != null && p.getUsuario().getId().equals(usuario.getId())
                             && p.getEstado() == EstadoPostulacion.PENDING)
                    .toList();
            
            if (!postulacionesExistentes.isEmpty()) {
                session.setAttribute("mensaje", "Ya te postulaste a este proyecto.");
                return "redirect:/postulaciones/" + id;
            }

            // Permitir reintentar si fue rechazada (eliminar la rechazada anterior)
            java.util.List<com.obratech.entity.Postulacion> postulacionesRechazadas = 
                postulacionRepository.findByProyectoId(id).stream()
                    .filter(p -> p.getUsuario() != null && p.getUsuario().getId().equals(usuario.getId())
                             && p.getEstado() == EstadoPostulacion.REJECTED)
                    .toList();
            
            for (com.obratech.entity.Postulacion pr : postulacionesRechazadas) {
                postulacionRepository.delete(pr);
            }

            // Guardar nueva postulación
            com.obratech.entity.Postulacion post = new com.obratech.entity.Postulacion();
            post.setProyecto(proyecto);
            post.setUsuario(usuario);
            post.setFechaPostulacion(java.time.LocalDateTime.now());
            
            postulacionRepository.save(post);

            // Mensaje simple en sesin para mostrar en el detalle
            session.setAttribute("mensaje", "Postulacin enviada exitosamente! El cliente revisar tu solicitud pronto.");
            // Redirigir al detalle del proyecto para ver confirmacin
            return "redirect:/postulaciones/" + id;
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar tu postulacin: " + e.getMessage());
            model.addAttribute("proyecto", proyecto);
            return "trabajadores/detalles-postulacion";
        }
    }

    // Ver todos los proyectos disponibles para postularse
    @GetMapping
    public String verProyectosDisponibles(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_WORKER") && !usuario.getRoles().contains("ROLE_CONTRACTOR"))) {
            return "redirect:/desboard";
        }

    List<Proyecto> proyectos = proyectoRepository.findByFechaLimitePostulacionIsNullOrFechaLimitePostulacionGreaterThanEqual(java.time.LocalDate.now());

        model.addAttribute("proyectos", proyectos);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalProyectos", proyectos.size());

        return "trabajadores/proyectos-disponibles";
    }

    // Handle POST to root (redirect to avoid 405)
    @PostMapping
    public String postulacionesRoot() {
        return "redirect:/postulaciones";
    }

    // Ver detalles de proyecto para postulacin - ONLY GET
    // Se expone en dos rutas para compatibilidad con plantillas:
    // GET /postulaciones/ver/{id} y GET /postulaciones/{id}
    @GetMapping({"/ver/{id}", "/{id}"})
    public String verDetallesProyecto(
            @PathVariable String id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_WORKER") && !usuario.getRoles().contains("ROLE_CONTRACTOR"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);

        if (proyecto == null) {
            return "redirect:/postulaciones";
        }

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("usuario", usuario);

        // Traspasar mensajes de sesión a modelo y limpiar de la sesión para evitar persistencia molesta
        if (session.getAttribute("mensaje") != null) {
            model.addAttribute("mensaje", session.getAttribute("mensaje"));
            session.removeAttribute("mensaje");
        }
        if (session.getAttribute("error") != null) {
            model.addAttribute("error", session.getAttribute("error"));
            session.removeAttribute("error");
        }

        return "trabajadores/detalles-postulacion";
    }

    // Ver postulantes de un proyecto (para el cliente/empresa)
    @GetMapping("/{id}/postulantes")
    public String verPostulantesProyecto(
            @PathVariable String id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        // Solo el cliente (empresa) puede ver los postulantes de su proyecto
        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_CLIENT") && !usuario.getRoles().contains("ROLE_ADMIN"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);
        if (proyecto == null) {
            return "redirect:/mis-proyectos";
        }

        // Cargar postulaciones
        java.util.List<com.obratech.entity.Postulacion> postulaciones = postulacionRepository.findByProyectoId(id);

        // Indicar si ya existe una postulacin aceptada para este proyecto
        boolean tieneAceptada = postulaciones.stream().anyMatch(p -> EstadoPostulacion.ACCEPTED.equals(p.getEstado()));

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("postulaciones", postulaciones);
        model.addAttribute("usuario", usuario);
        model.addAttribute("tieneAceptada", tieneAceptada);
        return "clientes/postulantes-proyecto";
    }

    // Ver todas las postulaciones de los proyectos del cliente (vista agregada)
    @GetMapping("/mis-postulaciones")
    public String verMisPostulaciones(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_CLIENT") && !usuario.getRoles().contains("ROLE_ADMIN"))) {
            return "redirect:/desboard";
        }

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(usuario);
        java.util.List<Proyecto> proyectos = proyectoRepository.findByClienteId(dbUsuario.getId());
        
        java.util.List<com.obratech.entity.Postulacion> todas = proyectos.isEmpty()
                ? new java.util.ArrayList<>()
                : postulacionRepository.findByProyectoIn(proyectos);

        model.addAttribute("postulaciones", todas);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalPostulaciones", todas.size());
        return "clientes/mis-postulaciones";
    }

    // Ver postulaciones de un contratista (sus propias postulaciones a proyectos)
    @GetMapping("/mis-postulaciones-contratista")
    public String verMisPostulacionesContratista(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        // Solo CONTRATISTA puede ver sus postulaciones
        if (usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) {
            return "redirect:/desboard";
        }

        java.util.List<com.obratech.entity.Postulacion> postulaciones = postulacionRepository.findByUsuarioId(usuario.getId());

        model.addAttribute("postulaciones", postulaciones);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalPostulaciones", postulaciones.size());
        return "trabajadores/mis-postulaciones";
    }

    // Aceptar una postulacion
    @PostMapping("/{proyectoId}/postulantes/{postId}/aceptar")
    public String aceptarPostulante(
            @PathVariable String proyectoId,
            @PathVariable String postId,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        // Solo cliente/admin y dueo del proyecto
        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_CLIENT") && !usuario.getRoles().contains("ROLE_ADMIN"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (proyecto == null) return "redirect:/mis-proyectos";

        // Verificar que el usuario es el dueo del proyecto o admin
        if ((usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_ADMIN")) && (proyecto.getCliente() == null || proyecto.getCliente().getUsername() == null || !proyecto.getCliente().getUsername().equals(usuario.getUsername()))) {
            return "redirect:/desboard";
        }

        com.obratech.entity.Postulacion post = postulacionRepository.findById(postId).orElse(null);
        if (post == null || post.getProyecto() == null || !post.getProyecto().getId().equals(proyectoId)) {
            return "redirect:/postulaciones/" + proyectoId + "/postulantes";
        }

        // Aceptar la postulacin y rechazar las dems para garantizar que solo haya UNA aceptada por proyecto
        try {
            // Marcar otras postulaciones como REJECTED
            java.util.List<com.obratech.entity.Postulacion> todas = postulacionRepository.findByProyectoId(proyectoId);
            for (com.obratech.entity.Postulacion p : todas) {
                if (!p.getId().equals(postId)) {
                    p.setEstado(EstadoPostulacion.REJECTED);
                    postulacionRepository.save(p);
                }
            }

            // Marcar la seleccionada como ACCEPTED
            post.setEstado(EstadoPostulacion.ACCEPTED);
            postulacionRepository.save(post);

            // Asignar contratista (Persona) al proyecto si existe
            String username = post.getUsuario() != null ? post.getUsuario().getUsername() : null;
            if (username != null) {
                Perfil cont = perfilRepository.findByUsernameIgnoreCase(username).orElse(null);
                if (cont != null && "ROLE_CONTRACTOR".equals(cont.getRole())) {
                    proyecto.setContratistaAsignado(cont);
                    proyecto.setEstadoAsignacion(EstadoAsignacion.SELECCIONADO_PENDIENTE_CONTRATACION);
                    proyectoRepository.save(proyecto);
                    session.setAttribute("mensaje", "Has seleccionado a " + username + " como asignado (pendiente contratacin).");
                }
            }
        } catch (Exception ex) {
            session.setAttribute("error", "Ocurri un error al aceptar la postulacin: " + ex.getMessage());
        }

        return "redirect:/postulaciones/" + proyectoId + "/postulantes";
    }

    // Rechazar una postulacion
    @PostMapping("/{proyectoId}/postulantes/{postId}/rechazar")
    public String rechazarPostulante(
            @PathVariable String proyectoId,
            @PathVariable String postId,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_CLIENT") && !usuario.getRoles().contains("ROLE_ADMIN"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (proyecto == null) return "redirect:/mis-proyectos";

        if ((usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_ADMIN")) && (proyecto.getCliente() == null || proyecto.getCliente().getUsername() == null || !proyecto.getCliente().getUsername().equals(usuario.getUsername()))) {
            return "redirect:/desboard";
        }

        com.obratech.entity.Postulacion post = postulacionRepository.findById(postId).orElse(null);
        if (post == null || post.getProyecto() == null || !post.getProyecto().getId().equals(proyectoId)) {
            return "redirect:/postulaciones/" + proyectoId + "/postulantes";
        }

        post.setEstado(EstadoPostulacion.REJECTED);
        postulacionRepository.save(post);

        // Notificacin eliminada (funcionalidad removida)

        return "redirect:/postulaciones/" + proyectoId + "/postulantes";
    }

    // Eliminar una postulacion
    @PostMapping("/{proyectoId}/postulantes/{postId}/eliminar")
    public String eliminarPostulante(
            @PathVariable String proyectoId,
            @PathVariable String postId,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_CLIENT") && !usuario.getRoles().contains("ROLE_ADMIN"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (proyecto == null) return "redirect:/mis-proyectos";

        if ((usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_ADMIN")) && (proyecto.getCliente() == null || proyecto.getCliente().getUsername() == null || !proyecto.getCliente().getUsername().equals(usuario.getUsername()))) {
            return "redirect:/desboard";
        }

        com.obratech.entity.Postulacion post = postulacionRepository.findById(postId).orElse(null);
        if (post == null || post.getProyecto() == null || !post.getProyecto().getId().equals(proyectoId)) {
            return "redirect:/postulaciones/" + proyectoId + "/postulantes";
        }

        // Si el contratista eliminado era el asignado, desasignarlo
        if (proyecto.getContratistaAsignado() != null && 
            post.getUsuario() != null &&
            proyecto.getContratistaAsignado().getUsername() != null &&
            proyecto.getContratistaAsignado().getUsername().equals(post.getUsuario().getUsername())) {
            proyecto.setContratistaAsignado(null);
            proyecto.setEstadoAsignacion(EstadoAsignacion.SIN_ASIGNAR);
            proyectoRepository.save(proyecto);
        }

        postulacionRepository.delete(post);
        session.setAttribute("mensaje", "Postulacin eliminada exitosamente.");

        return "redirect:/postulaciones/mis-postulaciones";
    }
}

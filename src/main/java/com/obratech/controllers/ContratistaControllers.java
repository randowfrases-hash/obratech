package com.obratech.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.entity.InvitacionTrabajo;
import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.InvitacionTrabajoRepository;
import com.obratech.repository.EquipoTrabajoRepository;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import com.obratech.entity.EquipoTrabajo;

@Controller
@RequestMapping("/contratistas")
public class ContratistaControllers {

    @Autowired private PerfilRepository perfilRepository;
    @Autowired private CalificacionRepository calificacionRepository;
    @Autowired private ProyectoRepository proyectoRepository;
    @Autowired private InvitacionTrabajoRepository invitacionTrabajoRepository;
    @Autowired private EquipoTrabajoRepository equipoTrabajoRepository;
    @Autowired private org.springframework.data.mongodb.gridfs.GridFsTemplate gridFsTemplate;

    // Listar todos los contratistas activos
    @GetMapping
    public String listarContratistas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        List<Perfil> contratistas = perfilRepository.findByRolesAndActivoTrue("ROLE_CONTRACTOR");
        model.addAttribute("contratistas", contratistas);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalContratistas", contratistas.size());
        return "listar-contratistas";
    }

    // Ver detalles de un contratista por ID
    @GetMapping("/{id}")
    public String verDetallesContratista(@PathVariable String id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        Perfil contratista = perfilRepository.findById(safeId).orElse(null);
        if (contratista == null) return "redirect:/contratistas";

        model.addAttribute("contratista", contratista);

        if (contratista.getEmail() != null) {
            perfilRepository.findByEmailIgnoreCase(contratista.getEmail()).ifPresent(p -> {
                model.addAttribute("calificaciones", calificacionRepository.findByContratistaId(p.getId()));
                model.addAttribute("personaId", p.getId());
            });
        }

        model.addAttribute("usuario", usuario);
        return "detalles-contratista";
    }

    // Ver detalles por username
    @GetMapping("/por-username/{username}")
    public String verPorUsername(@PathVariable String username, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (contratista == null) return "redirect:/contratistas";

        model.addAttribute("contratista", contratista);

        if (contratista.getEmail() != null) {
            perfilRepository.findByEmailIgnoreCase(contratista.getEmail()).ifPresent(p -> {
                model.addAttribute("calificaciones", calificacionRepository.findByContratistaId(p.getId()));
                model.addAttribute("personaId", p.getId());
            });
        }

        model.addAttribute("usuario", usuario);
        return "detalles-contratista";
    }

    // El contratista edita su propio perfil  GET
    @GetMapping("/editar")
    public String mostrarFormularioEditarPerfil(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null) return "redirect:/perfil-contratista";

        model.addAttribute("contratista", contratista);
        model.addAttribute("usuario", usuario);
        return "editar-contratista";
    }

    // El contratista edita su propio perfil  POST
    @PostMapping("/editar")
    public String guardarPerfilPropio(
            @ModelAttribute Perfil form,
            @RequestParam(value = "cvFile", required = false) org.springframework.web.multipart.MultipartFile cvFile,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null) return "redirect:/perfil-contratista";

        contratista.setNombre(form.getNombre());
        contratista.setApellido(form.getApellido());
        contratista.setEmail(form.getEmail());
        contratista.setTelefono(form.getTelefono());
        contratista.setEspecialidad(form.getEspecialidad());
        contratista.setDescripcion(form.getDescripcion());
        contratista.setUbicacion(form.getUbicacion());
        if (form.getExperiencia() != null && form.getExperiencia() >= 0)
            contratista.setExperiencia(form.getExperiencia());

        // Subir CV si se proporcion
        if (cvFile != null && !cvFile.isEmpty()) {
            try {
                String fileName = java.util.UUID.randomUUID() + "_cv_" + cvFile.getOriginalFilename();
                org.bson.types.ObjectId fileId = gridFsTemplate.store(
                        cvFile.getInputStream(), fileName, cvFile.getContentType());
                contratista.setCvUrl(fileId.toString());
            } catch (java.io.IOException e) {
                // Si falla el CV, igualmente guardar los datos del perfil
                System.err.println("Error uploading CV: " + e.getMessage());
            }
        }

        perfilRepository.save(contratista);
        return "redirect:/perfil-contratista?exito=true";
    }

    // Mostrar formulario para contratar a un trabajador
    @GetMapping("/contratar/{trabajadorId}")
    public String formularioContratar(@PathVariable String trabajadorId, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null) return "redirect:/desboard";

        Perfil trabajador = perfilRepository.findById(trabajadorId).orElse(null);
        if (trabajador == null) return "redirect:/trabajadores";

        // Proyectos activos del contratista
        List<com.obratech.entity.Proyecto> proyectosActivos = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucionNot(contratista.getId(), EstadoEjecucion.COMPLETADO);

        model.addAttribute("usuario", usuario);
        model.addAttribute("trabajador", trabajador);
        model.addAttribute("proyectosActivos", proyectosActivos);
        return "seleccionar-proyecto-contrato";
    }

    // Procesar asignación de trabajador a proyecto
    @PostMapping("/contratar/{trabajadorId}")
    public String contratarTrabajador(
            @PathVariable String trabajadorId,
            @RequestParam("proyectoId") String proyectoId,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        @SuppressWarnings("null")
        String safeTId = trabajadorId != null ? trabajadorId : "";
        Perfil trabajador = perfilRepository.findById(safeTId).orElse(null);
        @SuppressWarnings("null")
        String safePId = proyectoId != null ? proyectoId : "";
        com.obratech.entity.Proyecto proyecto = proyectoRepository.findById(safePId).orElse(null);

        if (contratista == null || trabajador == null || proyecto == null) {
            return "redirect:/trabajadores";
        }

        if (proyecto.getContratistaAsignado() != null
                && proyecto.getContratistaAsignado().getId().equals(contratista.getId())) {

            // Verificar si ya está en el equipo (pool)
            boolean yaEnProyecto = proyecto.getEquipoTrabajo() != null && proyecto.getEquipoTrabajo().stream()
                    .anyMatch(t -> t.getId() != null && t.getId().equals(trabajador.getId()));

            if (yaEnProyecto) {
                return "redirect:/contratistas/contratar/" + trabajadorId + "?error_contrato=ya_en_proyecto";
            }

            // Verificar si ya tiene invitación pendiente para este proyecto
            List<InvitacionTrabajo> pendientes = invitacionTrabajoRepository.findByTrabajadorIdAndEstado(trabajador.getId(), "PENDIENTE");
            boolean yaInvitado = pendientes.stream().anyMatch(inv -> inv.getProyecto() != null && inv.getProyecto().getId().equals(proyecto.getId()));
            if (yaInvitado) {
                return "redirect:/contratistas/contratar/" + trabajadorId + "?error_contrato=ya_invitado";
            }

            // Crear invitación de trabajo
            InvitacionTrabajo invitacion = new InvitacionTrabajo();
            invitacion.setProyecto(proyecto);
            invitacion.setTrabajador(trabajador);
            invitacion.setContratista(contratista);
            invitacion.setEstado("PENDIENTE");
            invitacion.setFechaCreacion(java.time.LocalDateTime.now());

            invitacionTrabajoRepository.save(invitacion);

            return "redirect:/proyectos/" + proyectoId + "?invitacion_enviada=true";
        }

        return "redirect:/contratistas/proyectos-asignados";
    }

    // Crear un nuevo Equipo de Trabajo
    @PostMapping("/proyectos/{proyectoId}/equipos/crear")
    public String crearEquipoTrabajo(
            @PathVariable String proyectoId,
            @RequestParam("nombre") String nombre,
            @RequestParam("actividad") String actividad,
            @RequestParam(value = "porcentajeAvance", defaultValue = "0.0") Double porcentajeAvance,
            @RequestParam(value = "integranteIds", required = false) List<String> integranteIds,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        com.obratech.entity.Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);

        if (contratista == null || proyecto == null) {
            return "redirect:/desboard";
        }

        // Verificar pertenencia y permisos del contratista
        if (proyecto.getContratistaAsignado() == null || !proyecto.getContratistaAsignado().getId().equals(contratista.getId())) {
            return "redirect:/desboard";
        }

        // Validación: mínimo 1 integrante
        if (integranteIds == null || integranteIds.isEmpty()) {
            return "redirect:/proyectos/" + proyectoId + "?error_equipo=minimo_un_integrante";
        }

        List<Perfil> integrantes = new java.util.ArrayList<>();
        for (String id : integranteIds) {
            Perfil integrante = perfilRepository.findById(id).orElse(null);
            if (integrante == null) {
                return "redirect:/proyectos/" + proyectoId + "?error_equipo=integrante_invalido";
            }
            // Verificar que pertenezca al pool del proyecto
            boolean perteneceAlPool = proyecto.getEquipoTrabajo().stream()
                    .anyMatch(t -> t.getId() != null && t.getId().equals(integrante.getId()));
            if (!perteneceAlPool) {
                return "redirect:/proyectos/" + proyectoId + "?error_equipo=integrante_fuera_de_pool";
            }
            integrantes.add(integrante);
        }

        EquipoTrabajo nuevoEquipo = new EquipoTrabajo();
        nuevoEquipo.setNombre(nombre);
        nuevoEquipo.setActividad(actividad);
        nuevoEquipo.setPorcentajeAvance(porcentajeAvance);
        nuevoEquipo.setProyecto(proyecto);
        nuevoEquipo.setIntegrantes(integrantes);

        equipoTrabajoRepository.save(nuevoEquipo);

        return "redirect:/proyectos/" + proyectoId + "?equipo_creado=true";
    }

    // Eliminar un Equipo de Trabajo
    @PostMapping("/proyectos/{proyectoId}/equipos/{equipoId}/eliminar")
    public String eliminarEquipoTrabajo(
            @PathVariable String proyectoId,
            @PathVariable String equipoId,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        com.obratech.entity.Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);

        if (contratista == null || proyecto == null) {
            return "redirect:/desboard";
        }

        // Verificar pertenencia
        if (proyecto.getContratistaAsignado() == null || !proyecto.getContratistaAsignado().getId().equals(contratista.getId())) {
            return "redirect:/desboard";
        }

        equipoTrabajoRepository.deleteById(equipoId);

        return "redirect:/proyectos/" + proyectoId + "?equipo_eliminado=true";
    }

    // Actualizar avance de un Equipo de Trabajo
    @PostMapping("/proyectos/{proyectoId}/equipos/{equipoId}/actualizar-avance")
    public String actualizarAvanceEquipo(
            @PathVariable String proyectoId,
            @PathVariable String equipoId,
            @RequestParam("porcentajeAvance") Double porcentajeAvance,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        com.obratech.entity.Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);

        if (contratista == null || proyecto == null) {
            return "redirect:/desboard";
        }

        // Verificar pertenencia
        if (proyecto.getContratistaAsignado() == null || !proyecto.getContratistaAsignado().getId().equals(contratista.getId())) {
            return "redirect:/desboard";
        }

        EquipoTrabajo equipo = equipoTrabajoRepository.findById(equipoId).orElse(null);
        if (equipo != null) {
            equipo.setPorcentajeAvance(porcentajeAvance);
            equipoTrabajoRepository.save(equipo);
        }

        return "redirect:/proyectos/" + proyectoId + "?avance_actualizado=true";
    }

    // Proyectos asignados al contratista
    @GetMapping("/proyectos-asignados")
    public String proyectosAsignados(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null) return "redirect:/desboard";

        List<com.obratech.entity.Proyecto> proyectos = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucionNot(contratista.getId(), EstadoEjecucion.COMPLETADO);
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("usuario", usuario);
        return "contratista-proyectos-asignados";
    }

    // Historial de proyectos completados
    @GetMapping("/historial")
    public String historialProyectos(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null) return "redirect:/desboard";

        List<com.obratech.entity.Proyecto> historial = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucion(contratista.getId(), EstadoEjecucion.COMPLETADO);
        model.addAttribute("proyectos", historial);
        model.addAttribute("usuario", usuario);
        model.addAttribute("esHistorial", true);
        return "contratista-historial-proyectos";
    }

    // Ver equipos de trabajo (vista contratista)
    @GetMapping("/mi-equipo")
    public String verMisEquipos(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null) return "redirect:/desboard";

        // Recolectar equipos de los proyectos asignados al contratista
        java.util.List<com.obratech.entity.Proyecto> proyectos = proyectoRepository.findByContratistaAsignadoId(contratista.getId());
        java.util.List<EquipoTrabajo> equipos = new ArrayList<>();
        if (proyectos != null) {
            for (com.obratech.entity.Proyecto p : proyectos) {
                if (p == null || p.getId() == null) continue;
                java.util.List<EquipoTrabajo> porProyecto = equipoTrabajoRepository.findByProyectoId(p.getId());
                if (porProyecto != null && !porProyecto.isEmpty()) equipos.addAll(porProyecto);
            }
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("contratista", contratista);
        model.addAttribute("equipos", equipos);
        return "contratistas/contratista-equipos";
    }

    // Buscar contratistas por especialidad
    @GetMapping("/buscar/{especialidad}")
    public String buscarPorEspecialidad(@PathVariable String especialidad, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        List<Perfil> contratistas = perfilRepository
                .findByEspecialidadContainingIgnoreCaseAndRoles(especialidad, "ROLE_CONTRACTOR");

        model.addAttribute("contratistas", contratistas);
        model.addAttribute("usuario", usuario);
        model.addAttribute("especialidad", especialidad);
        model.addAttribute("totalContratistas", contratistas.size());
        return "listar-contratistas";
    }

    @GetMapping("/para-calificar/{proyectoId}")
    public String contratistasParaCalificar(@PathVariable String proyectoId, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        List<Perfil> contratistas = perfilRepository.findByRolesAndActivoTrue("ROLE_CONTRACTOR");
        model.addAttribute("contratistas", contratistas);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalContratistas", contratistas.size());
        model.addAttribute("proyectoId", proyectoId);
        return "clientes/contratistas-para-calificar";
    }
}

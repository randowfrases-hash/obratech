package com.obratech.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.enums.EstadoAsignacion;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.service.ProyectoService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/proyectos")
public class ProyectoControllers {

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private com.obratech.repository.UsuarioRepository usuarioRepository;

    @Autowired
    private EquipoTrabajoRepository equipoTrabajoRepository;

    @GetMapping("/publicar")
    public String mostrarFormularioProyecto(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        // Consultar el estado real en la BD
        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (dbUsuario == null || !dbUsuario.isVerificado()) {
            return "redirect:/desboard?errorVerificacion=true";
        }

        model.addAttribute("proyecto", new Proyecto());
        model.addAttribute("usuario", dbUsuario);
        return "publicar-proyecto-new";
    }

    @PostMapping("/publicar")
    public String publicarProyecto(
            @ModelAttribute Proyecto proyecto,
            @RequestParam(value = "documentoLegal", required = false) MultipartFile documentoLegal,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        // Consultar el estado real en la BD
        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (dbUsuario == null || !dbUsuario.isVerificado()) {
            return "redirect:/desboard?errorVerificacion=true";
        }

        proyecto.setEstadoAsignacion(EstadoAsignacion.SIN_ASIGNAR);
        proyecto.setEstadoEjecucion(EstadoEjecucion.PENDIENTE);
        proyecto.setFechaCreacion(java.time.LocalDateTime.now());
        proyecto.setCliente(dbUsuario);

        if (proyecto.getFechaInicio() == null) {
            proyecto.setFechaInicio(LocalDate.now());
        }

        if (documentoLegal != null && !documentoLegal.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + documentoLegal.getOriginalFilename();
                org.bson.types.ObjectId fileId = gridFsTemplate.store(
                        documentoLegal.getInputStream(), fileName, documentoLegal.getContentType());
                proyecto.setDocumentoLegalUrl(fileId.toString());
                proyecto.setDocumentoLegalNombre(documentoLegal.getOriginalFilename());
            } catch (Exception e) {
                model.addAttribute("error", "Error al subir el documento: " + e.getMessage());
                model.addAttribute("proyecto", proyecto);
                model.addAttribute("usuario", dbUsuario);
                return "publicar-proyecto-new";
            }
        }

        proyectoService.publicarProyecto(proyecto, usuario);
        return "redirect:/proyectos/mis-proyectos";
    }

    // Ver todos los proyectos del cliente actual
    @GetMapping("/mis-proyectos")
    public String verMisProyectos(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(usuario);
        List<Proyecto> proyectos = proyectoRepository.findByClienteId(dbUsuario.getId());
        if (proyectos == null) {
            proyectos = new ArrayList<>();
        }

        long enProgreso = proyectoRepository.countByClienteIdAndEstadoEjecucion(usuario.getId(), EstadoEjecucion.EN_PROGRESO);
        long completados = proyectoRepository.countByClienteIdAndEstadoEjecucion(usuario.getId(), EstadoEjecucion.COMPLETADO);

        model.addAttribute("proyectos", proyectos);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalProyectos", proyectoRepository.countByClienteId(usuario.getId()));
        model.addAttribute("proyectosEnProgreso", enProgreso);
        model.addAttribute("proyectosCompletados", completados);

        return "mis-proyectos";
    }

    // Ver detalles de un proyecto
    @GetMapping("/{id}")
    public String verDetallesProyecto(
            @PathVariable String id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

    @SuppressWarnings("null")
    String safeId1 = id != null ? id : "";
    Proyecto proyecto = proyectoRepository.findById(safeId1).orElse(null);

    // Allow access if the session user is the project owner (cliente)
    boolean isOwner = proyecto != null && proyecto.getCliente() != null
        && proyecto.getCliente().getUsername() != null
        && proyecto.getCliente().getUsername().equalsIgnoreCase(usuario.getUsername());

    // Also allow access if the session user is the contratista assigned to the project
    boolean isAssignedContractor = proyecto != null && proyecto.getContratistaAsignado() != null
        && proyecto.getContratistaAsignado().getUsername() != null
        && proyecto.getContratistaAsignado().getUsername().equalsIgnoreCase(usuario.getUsername());

    boolean isAdmin = usuario.getRoles() != null && usuario.getRoles().contains("ROLE_ADMIN");

    if (proyecto == null || !(isOwner || isAssignedContractor || isAdmin)) {
        return "redirect:/proyectos/mis-proyectos";
    }

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("usuario", usuario);
        List<EquipoTrabajo> equipos = equipoTrabajoRepository.findByProyectoId(proyecto.getId());
        model.addAttribute("equipos", equipos);
        return "detalles-proyecto";
    }

    // Editar proyecto
    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(
            @PathVariable String id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        @SuppressWarnings("null")
        String safeId3 = id != null ? id : "";
        Proyecto proyecto = proyectoRepository.findById(safeId3).orElse(null);

     if (proyecto == null || 
         proyecto.getCliente() == null || proyecto.getCliente().getUsername() == null || !proyecto.getCliente().getUsername().equals(usuario.getUsername())) {
            return "redirect:/proyectos/mis-proyectos";
        }

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("usuario", usuario);
        return "editar-proyecto";
    }

    // Guardar cambios de proyecto
    @PostMapping("/{id}/editar")
    public String editarProyecto(
            @PathVariable String id,
            @ModelAttribute Proyecto proyectoEditado,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        @SuppressWarnings("null")
        String safeId4 = id != null ? id : "";
        Proyecto proyecto = proyectoRepository.findById(safeId4).orElse(null);

        if (proyecto == null || 
            proyecto.getCliente() == null || proyecto.getCliente().getUsername() == null || !proyecto.getCliente().getUsername().equals(usuario.getUsername())) {
            return "redirect:/proyectos/mis-proyectos";
        }
        proyecto.setTitulo(proyectoEditado.getTitulo());
        proyecto.setDescripcion(proyectoEditado.getDescripcion());
        proyecto.setUbicacion(proyectoEditado.getUbicacion());
        proyecto.setTipoProyecto(proyectoEditado.getTipoProyecto());
        proyecto.setPresupuesto(proyectoEditado.getPresupuesto());
        proyecto.setFechaInicio(proyectoEditado.getFechaInicio());
        proyecto.setFechaEntrega(proyectoEditado.getFechaEntrega());
        proyecto.setPlazoEstimado(proyectoEditado.getPlazoEstimado());
        proyecto.setAreaTotal(proyectoEditado.getAreaTotal());
        proyecto.setObservaciones(proyectoEditado.getObservaciones());

        proyectoRepository.save(proyecto);

        return "redirect:/proyectos/" + id;
    }

    // Eliminar proyecto
    @PostMapping("/{id}/eliminar")
    public String eliminarProyecto(
            @PathVariable String id,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);

        if (proyecto != null && (proyecto.getCliente() == null || (proyecto.getCliente().getUsername() != null && proyecto.getCliente().getUsername().equals(usuario.getUsername())))) {
            @SuppressWarnings("null")
            String safeId2 = id != null ? id : "";
            proyectoRepository.deleteById(safeId2);
        }

        return "redirect:/proyectos/mis-proyectos";
    }

    @GetMapping("/documento/{id}")
    public void downloadDocumento(@PathVariable String id, jakarta.servlet.http.HttpServletResponse response) {
        try {
            com.mongodb.client.gridfs.model.GridFSFile file = gridFsTemplate.findOne(
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id)));
            if (file != null) {
                response.setContentType(gridFsTemplate.getResource(file).getContentType());
                response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"");
                gridFsTemplate.getResource(file).getInputStream().transferTo(response.getOutputStream());
            }
        } catch (Exception e) {
            // Error handling
        }
    }
}

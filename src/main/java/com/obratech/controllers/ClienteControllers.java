package com.obratech.controllers;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/clientes")
public class ClienteControllers {

    @Autowired private PerfilRepository perfilRepository;
    @Autowired private ProyectoRepository proyectoRepository;
    @Autowired private CalificacionRepository calificacionRepository;
    @Autowired private com.obratech.repository.UsuarioRepository usuarioRepository;

    @GetMapping
    public String listarClientes(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        List<Perfil> clientes = perfilRepository.findByRolesAndActivoTrue("ROLE_CLIENT");
        model.addAttribute("clientes", clientes);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalClientes", clientes.size());
        return "listar-clientes";
    }

    @GetMapping("/mis-contratistas")
    public String misContratistas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CLIENT")) return "redirect:/login";

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(usuario);
        List<Proyecto> proyectosDelCliente = proyectoRepository.findByClienteId(dbUsuario.getId());
        if (proyectosDelCliente == null) {
            proyectosDelCliente = new ArrayList<>();
        }

        List<Perfil> contratistas = proyectosDelCliente.stream()
                .filter(p -> p.getContratistaAsignado() != null)
                .map(Proyecto::getContratistaAsignado)
                .distinct()
                .collect(Collectors.toList());

        model.addAttribute("usuario", usuario);
        model.addAttribute("proyectos", proyectosDelCliente);
        model.addAttribute("contratistas", contratistas);
        return "clientes/mis-contratistas";
    }

    @GetMapping("/proyectos-en-proceso")
    public String proyectosEnProceso(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CLIENT")) return "redirect:/login";

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(usuario);
        List<Proyecto> proyectosDelCliente = proyectoRepository.findByClienteId(dbUsuario.getId());
        if (proyectosDelCliente == null) {
            proyectosDelCliente = new ArrayList<>();
        }

        List<Proyecto> proyectosEnProceso = proyectosDelCliente.stream()
                .filter(p -> p.getEstadoEjecucion() != null && p.getEstadoEjecucion() != EstadoEjecucion.COMPLETADO)
                .collect(Collectors.toList());

        model.addAttribute("usuario", usuario);
        model.addAttribute("proyectos", proyectosEnProceso);
        model.addAttribute("totalProyectos", proyectosEnProceso.size());
        model.addAttribute("proyectosEnProgreso", proyectosEnProceso.size());
        model.addAttribute("proyectosCompletados", proyectoRepository.countByClienteIdAndEstadoEjecucion(usuario.getId(), EstadoEjecucion.COMPLETADO));
        model.addAttribute("proyectosPendientes", proyectoRepository.countByClienteIdAndEstadoEjecucion(usuario.getId(), EstadoEjecucion.PENDIENTE));
        return "clientes/proyectos-en-proceso";
    }

    @GetMapping("/reportes")
    public String reportes(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CLIENT")) return "redirect:/login";

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(usuario);
        List<Proyecto> todosLosProyectos = proyectoRepository.findByClienteId(dbUsuario.getId());
        if (todosLosProyectos == null) {
            todosLosProyectos = new ArrayList<>();
        }

        long completados    = proyectoRepository.countByClienteIdAndEstadoEjecucion(usuario.getId(), EstadoEjecucion.COMPLETADO);
        long enProgreso     = proyectoRepository.countByClienteIdAndEstadoEjecucion(usuario.getId(), EstadoEjecucion.EN_PROGRESO);
        long pendientes     = proyectoRepository.countByClienteIdAndEstadoEjecucion(usuario.getId(), EstadoEjecucion.PENDIENTE);
        long conContratista = proyectoRepository.countByClienteIdAndContratistaAsignadoIsNotNull(usuario.getId());
        long calificaciones = calificacionRepository.count();

        model.addAttribute("usuario", usuario);
        model.addAttribute("totalProyectos", proyectoRepository.countByClienteId(usuario.getId()));
        model.addAttribute("proyectosCompletados", completados);
        model.addAttribute("proyectosEnProgreso", enProgreso);
        model.addAttribute("proyectosPendientes", pendientes);
        model.addAttribute("proyectosConContratista", conContratista);
        model.addAttribute("calificacionesDadas", calificaciones);
        model.addAttribute("proyectos", todosLosProyectos);
        return "clientes/reportes";
    }
}

package com.obratech.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.obratech.entity.Usuario;
import com.obratech.service.UsuarioService;

/**
 *Gestin de usuarios y roles 
 */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Lista todos los usuarios del sistema */
    @GetMapping
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioService.findAll();
        model.addAttribute("usuarios", usuarios);
        return "admin/usuarios";
    }

    /** Activa o desactiva una cuenta */
    @PostMapping("/{id}/toggle-activo")
    public String toggleActivo(@PathVariable String id, RedirectAttributes ra) {
        usuarioService.toggleActivo(id);
        ra.addFlashAttribute("mensajeExito", "Estado de la cuenta actualizado correctamente.");
        return "redirect:/admin/usuarios";
    }

    /** Elimina un usuario  */
    @PostMapping("/{id}/eliminar")
    public String eliminarUsuario(@PathVariable String id, RedirectAttributes ra) {
        usuarioService.deleteById(id);
        ra.addFlashAttribute("mensajeExito", "Usuario eliminado del sistema.");
        return "redirect:/admin/usuarios";
    }
}

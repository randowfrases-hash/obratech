package com.obratech.service;

import com.obratech.dto.FiltroUsuarioDTO;
import com.obratech.entity.Usuario;
import com.obratech.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de búsqueda avanzada para usuarios
 * Proporciona métodos para filtrar usuarios por múltiples criterios
 */
@Service
public class BusquedaUsuarioService {

    private final UsuarioRepository usuarioRepository;

    public BusquedaUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Busca usuarios sin paginación
     */
    public List<Usuario> buscar(FiltroUsuarioDTO filtro) {
        return usuarioRepository.buscarConFiltros(filtro);
    }

    /**
     * Busca usuarios con paginación
     */
    public Page<Usuario> buscarPaginado(FiltroUsuarioDTO filtro) {
        Pageable pageable = PageRequest.of(filtro.getPagina(), filtro.getTamaño());
        return usuarioRepository.buscarConFiltrosPaginado(filtro, pageable);
    }

    /**
     * Busca usuarios por rol
     */
    public List<Usuario> buscarPorRol(String rol) {
        return usuarioRepository.buscarPorRol(rol);
    }

    /**
     * Busca solo usuarios verificados
     */
    public List<Usuario> buscarVerificados() {
        return usuarioRepository.buscarVerificados();
    }

    /**
     * Busca solo usuarios activos
     */
    public List<Usuario> buscarActivos() {
        return usuarioRepository.buscarActivos();
    }

    /**
     * Obtiene roles disponibles en el sistema
     */
    public List<String> obtenerRolesDisponibles() {
        return List.of("ROLE_ADMIN", "ROLE_CLIENT", "ROLE_CONTRACTOR", "ROLE_WORKER");
    }

    /**
     * Cuenta usuarios verificados
     */
    public long contarVerificados() {
        return usuarioRepository.buscarVerificados().size();
    }

    /**
     * Cuenta usuarios totales
     */
    public long contarTotales() {
        return usuarioRepository.count();
    }
}

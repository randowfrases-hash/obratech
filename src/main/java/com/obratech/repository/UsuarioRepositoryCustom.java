package com.obratech.repository;

import com.obratech.dto.FiltroUsuarioDTO;
import com.obratech.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Interfaz custom para búsquedas avanzadas de usuarios
 */
@NoRepositoryBean
public interface UsuarioRepositoryCustom {
    /**
     * Busca usuarios aplicando múltiples filtros simultáneamente
     * @param filtro DTO con criterios de búsqueda
     * @return Lista de usuarios que cumplen con los criterios
     */
    List<Usuario> buscarConFiltros(FiltroUsuarioDTO filtro);
    
    /**
     * Busca usuarios paginados con filtros
     * @param filtro DTO con criterios de búsqueda
     * @param pageable Información de paginación
     * @return Page de usuarios
     */
    Page<Usuario> buscarConFiltrosPaginado(FiltroUsuarioDTO filtro, Pageable pageable);
    
    /**
     * Busca usuarios por rol
     * @param rol El rol a buscar
     * @return Lista de usuarios
     */
    List<Usuario> buscarPorRol(String rol);
    
    /**
     * Busca solo usuarios verificados
     * @return Lista de usuarios verificados
     */
    List<Usuario> buscarVerificados();
    
    /**
     * Busca usuarios activos
     * @return Lista de usuarios activos
     */
    List<Usuario> buscarActivos();
}

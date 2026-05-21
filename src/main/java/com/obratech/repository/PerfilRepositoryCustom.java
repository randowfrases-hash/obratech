package com.obratech.repository;

import com.obratech.dto.FiltroContratistaDTO;
import com.obratech.entity.Perfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Interfaz custom para búsquedas avanzadas de contratistas/perfiles
 */
@NoRepositoryBean
public interface PerfilRepositoryCustom {
    /**
     * Busca contratistas aplicando múltiples filtros simultáneamente
     * @param filtro DTO con criterios de búsqueda
     * @return Lista de contratistas que cumplen con los criterios
     */
    List<Perfil> buscarContratistaConFiltros(FiltroContratistaDTO filtro);
    
    /**
     * Busca contratistas paginados con filtros
     * @param filtro DTO con criterios de búsqueda
     * @param pageable Información de paginación
     * @return Page de contratistas
     */
    Page<Perfil> buscarContratistaConFiltrosPaginado(FiltroContratistaDTO filtro, Pageable pageable);
    
    /**
     * Busca contratistas por especialidad
     * @param especialidad La especialidad a buscar
     * @return Lista de contratistas
     */
    List<Perfil> buscarPorEspecialidad(String especialidad);
    
    /**
     * Busca contratistas por nivel de experiencia
     * @param experiencia El nivel de experiencia
     * @return Lista de contratistas
     */
    List<Perfil> buscarPorExperiencia(String experiencia);
    
    /**
     * Busca contratistas verificados
     * @return Lista de contratistas verificados
     */
    List<Perfil> buscarContratistasVerificados();
    
    /**
     * Busca perfiles activos por rol
     * @param rol El rol a buscar
     * @return Lista de perfiles
     */
    List<Perfil> buscarActivosPorRol(String rol);
}

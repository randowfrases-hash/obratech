package com.obratech.repository;

import com.obratech.dto.FiltroProyectoDTO;
import com.obratech.entity.Proyecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Interfaz custom para búsquedas avanzadas de proyectos
 */
@NoRepositoryBean
public interface ProyectoRepositoryCustom {
    /**
     * Busca proyectos aplicando múltiples filtros simultáneamente
     * @param filtro DTO con criterios de búsqueda
     * @return Lista de proyectos que cumplen con los criterios
     */
    List<Proyecto> buscarConFiltros(FiltroProyectoDTO filtro);
    
    /**
     * Busca proyectos paginados con filtros
     * @param filtro DTO con criterios de búsqueda
     * @param pageable Información de paginación
     * @return Page de proyectos
     */
    Page<Proyecto> buscarConFiltrosPaginado(FiltroProyectoDTO filtro, Pageable pageable);
    
    /**
     * Busca proyectos por estado
     * @param estado El estado a buscar
     * @return Lista de proyectos
     */
    List<Proyecto> buscarPorEstado(String estado);
    
    /**
     * Busca proyectos por ubicación
     * @param ubicacion La ubicación a buscar
     * @return Lista de proyectos
     */
    List<Proyecto> buscarPorUbicacion(String ubicacion);
    
    /**
     * Busca proyectos dentro de un rango de presupuesto
     * @param presupuestoMin Presupuesto mínimo
     * @param presupuestoMax Presupuesto máximo
     * @return Lista de proyectos
     */
    List<Proyecto> buscarPorPresupuesto(Double presupuestoMin, Double presupuestoMax);
    
    /**
     * Busca proyectos por username del cliente usando query personalizada
     * @param clienteUsername El username del cliente
     * @return Lista de proyectos del cliente
     */
    List<Proyecto> findByClienteUsernameCustom(String clienteUsername);
}

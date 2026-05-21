package com.obratech.repository;

import com.obratech.dto.FiltroPostulacionDTO;
import com.obratech.entity.Postulacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Interfaz custom para búsquedas avanzadas de postulaciones
 */
@NoRepositoryBean
public interface PostulacionRepositoryCustom {
    /**
     * Busca postulaciones aplicando múltiples filtros simultáneamente
     * @param filtro DTO con criterios de búsqueda
     * @return Lista de postulaciones que cumplen con los criterios
     */
    List<Postulacion> buscarConFiltros(FiltroPostulacionDTO filtro);
    
    /**
     * Busca postulaciones paginadas con filtros
     * @param filtro DTO con criterios de búsqueda
     * @param pageable Información de paginación
     * @return Page de postulaciones
     */
    Page<Postulacion> buscarConFiltrosPaginado(FiltroPostulacionDTO filtro, Pageable pageable);
    
    /**
     * Busca postulaciones por estado
     * @param estado El estado a buscar
     * @return Lista de postulaciones
     */
    List<Postulacion> buscarPorEstado(String estado);
    
    /**
     * Busca postulaciones pendientes
     * @return Lista de postulaciones en estado pendiente
     */
    List<Postulacion> buscarPendientes();
    
    /**
     * Busca postulaciones aceptadas
     * @return Lista de postulaciones aceptadas
     */
    List<Postulacion> buscarAceptadas();
    
    /**
     * Busca postulaciones rechazadas
     * @return Lista de postulaciones rechazadas
     */
    List<Postulacion> buscarRechazadas();
}

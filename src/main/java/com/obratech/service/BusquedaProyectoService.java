package com.obratech.service;

import com.obratech.dto.FiltroProyectoDTO;
import com.obratech.entity.Proyecto;
import com.obratech.repository.ProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de búsqueda avanzada para proyectos
 * Proporciona métodos para filtrar proyectos por múltiples criterios
 */
@Service
public class BusquedaProyectoService {

    private final ProyectoRepository proyectoRepository;

    public BusquedaProyectoService(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    /**
     * Busca proyectos sin paginación
     */
    public List<Proyecto> buscar(FiltroProyectoDTO filtro) {
        return proyectoRepository.buscarConFiltros(filtro);
    }

    /**
     * Busca proyectos con paginación
     */
    public Page<Proyecto> buscarPaginado(FiltroProyectoDTO filtro) {
        Pageable pageable = PageRequest.of(filtro.getPagina(), filtro.getTamaño());
        return proyectoRepository.buscarConFiltrosPaginado(filtro, pageable);
    }

    /**
     * Busca proyectos por estado
     */
    public List<Proyecto> buscarPorEstado(String estado) {
        return proyectoRepository.buscarPorEstado(estado);
    }

    /**
     * Busca proyectos por ubicación
     */
    public List<Proyecto> buscarPorUbicacion(String ubicacion) {
        return proyectoRepository.buscarPorUbicacion(ubicacion);
    }

    /**
     * Busca proyectos por rango de presupuesto
     */
    public List<Proyecto> buscarPorPresupuesto(Double presupuestoMin, Double presupuestoMax) {
        return proyectoRepository.buscarPorPresupuesto(presupuestoMin, presupuestoMax);
    }

    /**
     * Obtiene todos los estados disponibles
     */
    public List<String> obtenerEstadosDisponibles() {
        return List.of("PENDIENTE", "EN_PROCESO", "FINALIZADO");
    }

    /**
     * Obtiene categorías/tipos de proyectos únicos
     */
    public List<String> obtenerCategoriasDisponibles() {
        // TODO: Implementar cuando se tengan datos
        return List.of("Construcción", "Remodelación", "Electricidad", "Plomería", "Carpintería");
    }

    /**
     * Obtiene ubicaciones únicas de proyectos
     */
    public List<String> obtenerUbicacionesDisponibles() {
        // TODO: Implementar cuando se tengan datos
        return List.of();
    }
}

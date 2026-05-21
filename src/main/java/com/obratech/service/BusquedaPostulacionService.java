package com.obratech.service;

import com.obratech.dto.FiltroPostulacionDTO;
import com.obratech.entity.Postulacion;
import com.obratech.repository.PostulacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de búsqueda avanzada para postulaciones
 * Proporciona métodos para filtrar postulaciones por múltiples criterios
 */
@Service
public class BusquedaPostulacionService {

    private final PostulacionRepository postulacionRepository;

    public BusquedaPostulacionService(PostulacionRepository postulacionRepository) {
        this.postulacionRepository = postulacionRepository;
    }

    /**
     * Busca postulaciones sin paginación
     */
    public List<Postulacion> buscar(FiltroPostulacionDTO filtro) {
        return postulacionRepository.buscarConFiltros(filtro);
    }

    /**
     * Busca postulaciones con paginación
     */
    public Page<Postulacion> buscarPaginado(FiltroPostulacionDTO filtro) {
        Pageable pageable = PageRequest.of(filtro.getPagina(), filtro.getTamaño());
        return postulacionRepository.buscarConFiltrosPaginado(filtro, pageable);
    }

    /**
     * Busca postulaciones por estado
     */
    public List<Postulacion> buscarPorEstado(String estado) {
        return postulacionRepository.buscarPorEstado(estado);
    }

    /**
     * Busca postulaciones pendientes
     */
    public List<Postulacion> buscarPendientes() {
        return postulacionRepository.buscarPendientes();
    }

    /**
     * Busca postulaciones aceptadas
     */
    public List<Postulacion> buscarAceptadas() {
        return postulacionRepository.buscarAceptadas();
    }

    /**
     * Busca postulaciones rechazadas
     */
    public List<Postulacion> buscarRechazadas() {
        return postulacionRepository.buscarRechazadas();
    }

    /**
     * Obtiene estados disponibles de postulaciones
     */
    public List<String> obtenerEstadosDisponibles() {
        return List.of("PENDING", "ACCEPTED", "REJECTED");
    }

    /**
     * Cuenta postulaciones pendientes
     */
    public long contarPendientes() {
        return postulacionRepository.buscarPendientes().size();
    }
}

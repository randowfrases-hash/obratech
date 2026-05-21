package com.obratech.service;

import com.obratech.dto.FiltroContratistaDTO;
import com.obratech.entity.Perfil;
import com.obratech.repository.PerfilRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de búsqueda avanzada para contratistas
 * Proporciona métodos para filtrar contratistas por múltiples criterios
 */
@Service
public class BusquedaContratistaService {

    private final PerfilRepository perfilRepository;

    public BusquedaContratistaService(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    /**
     * Busca contratistas sin paginación
     */
    public List<Perfil> buscar(FiltroContratistaDTO filtro) {
        return perfilRepository.buscarContratistaConFiltros(filtro);
    }

    /**
     * Busca contratistas con paginación
     */
    public Page<Perfil> buscarPaginado(FiltroContratistaDTO filtro) {
        Pageable pageable = PageRequest.of(filtro.getPagina(), filtro.getTamaño());
        return perfilRepository.buscarContratistaConFiltrosPaginado(filtro, pageable);
    }

    /**
     * Busca contratistas por especialidad
     */
    public List<Perfil> buscarPorEspecialidad(String especialidad) {
        return perfilRepository.buscarPorEspecialidad(especialidad);
    }

    /**
     * Busca contratistas por experiencia
     */
    public List<Perfil> buscarPorExperiencia(String experiencia) {
        return perfilRepository.buscarPorExperiencia(experiencia);
    }

    /**
     * Busca solo contratistas verificados
     */
    public List<Perfil> buscarVerificados() {
        return perfilRepository.buscarContratistasVerificados();
    }

    /**
     * Obtiene especialidades disponibles
     */
    public List<String> obtenerEspecialidadesDisponibles() {
        return List.of("Electricista", "Carpintero", "Plomero", "Pintor", "Constructor", "Arquitecto", "Ingeniero");
    }

    /**
     * Obtiene niveles de experiencia disponibles
     */
    public List<String> obtenerNivelesExperiencia() {
        return List.of("Junior", "Intermedio", "Senior");
    }

    /**
     * Obtiene estados de disponibilidad
     */
    public List<String> obtenerEstadosDisponibilidad() {
        return List.of("Disponible", "No disponible", "Parcialmente disponible");
    }
}

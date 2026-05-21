package com.obratech.repository;

import com.obratech.dto.FiltroContratistaDTO;
import com.obratech.entity.Perfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PerfilRepositoryImpl implements PerfilRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public PerfilRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Perfil> buscarContratistaConFiltros(FiltroContratistaDTO filtro) {
        Query query = construirQuery(filtro);
        @SuppressWarnings("null")
        List<Perfil> result = mongoTemplate.find(query, Perfil.class);
        return result;
    }

    @Override
    public Page<Perfil> buscarContratistaConFiltrosPaginado(FiltroContratistaDTO filtro, Pageable pageable) {
        Query query = construirQuery(filtro);
        
        @SuppressWarnings("null")
        long total = mongoTemplate.count(query, Perfil.class);
        
        @SuppressWarnings("null")
        Pageable safePageable = pageable != null ? pageable : org.springframework.data.domain.PageRequest.of(0, 20);
        query.with(safePageable);
        @SuppressWarnings("null")
        List<Perfil> contratistas = mongoTemplate.find(query, Perfil.class);
        
        return new PageImpl<>(contratistas, safePageable, total);
    }

    @Override
    public List<Perfil> buscarPorEspecialidad(String especialidad) {
        Query query = new Query();
        @SuppressWarnings("null")
        String safeEspecialidad = especialidad != null ? especialidad : "";
        query.addCriteria(Criteria.where("detallesContratista.especialidad").regex(safeEspecialidad, "i"));
        query.addCriteria(Criteria.where("roles").in("ROLE_CONTRACTOR"));
        @SuppressWarnings("null")
        List<Perfil> result = mongoTemplate.find(query, Perfil.class);
        return result;
    }

    @Override
    public List<Perfil> buscarPorExperiencia(String experiencia) {
        Query query = new Query();
        query.addCriteria(Criteria.where("detallesContratista.experiencia").is(experiencia));
        query.addCriteria(Criteria.where("roles").in("ROLE_CONTRACTOR"));
        @SuppressWarnings("null")
        List<Perfil> result = mongoTemplate.find(query, Perfil.class);
        return result;
    }

    @Override
    public List<Perfil> buscarContratistasVerificados() {
        Query query = new Query();
        query.addCriteria(Criteria.where("verificado").is(true));
        query.addCriteria(Criteria.where("roles").in("ROLE_CONTRACTOR"));
        query.addCriteria(Criteria.where("activo").is(true));
        @SuppressWarnings("null")
        List<Perfil> result = mongoTemplate.find(query, Perfil.class);
        return result;
    }

    @Override
    public List<Perfil> buscarActivosPorRol(String rol) {
        Query query = new Query();
        query.addCriteria(Criteria.where("roles").in(rol));
        query.addCriteria(Criteria.where("activo").is(true));
        @SuppressWarnings("null")
        List<Perfil> result = mongoTemplate.find(query, Perfil.class);
        return result;
    }

    /**
     * Construye la query de MongoDB basada en los criterios del filtro
     * Punto 13: Filtros avanzados para contratistas, clientes y trabajadores
     */
    private Query construirQuery(FiltroContratistaDTO filtro) {
        Query query = new Query();
        
        // Solo contratistas
        query.addCriteria(Criteria.where("roles").in("ROLE_CONTRACTOR"));
        
        // Filtro por especialidad
        if (filtro.getEspecialidad() != null && !filtro.getEspecialidad().isEmpty()) {
            query.addCriteria(Criteria.where("detallesContratista.especialidad").regex(filtro.getEspecialidad(), "i"));
        }
        
        // Filtro por experiencia
        if (filtro.getExperiencia() != null && !filtro.getExperiencia().isEmpty()) {
            query.addCriteria(Criteria.where("detallesContratista.experiencia").is(filtro.getExperiencia()));
        }
        
        // Filtro por calificación
        if (filtro.getCalificacionMin() != null || filtro.getCalificacionMax() != null) {
            Criteria calificacionCriteria = Criteria.where("calificacionPromedio");
            
            if (filtro.getCalificacionMin() != null) {
                @SuppressWarnings("null")
                Double minVal = filtro.getCalificacionMin();
                calificacionCriteria = calificacionCriteria.gte(minVal);
            }
            
            if (filtro.getCalificacionMax() != null) {
                @SuppressWarnings("null")
                Double maxVal = filtro.getCalificacionMax();
                calificacionCriteria = calificacionCriteria.lte(maxVal);
            }
            
            query.addCriteria(calificacionCriteria);
        }
        
        // Filtro por disponibilidad
        if (filtro.getDisponibilidad() != null && !filtro.getDisponibilidad().isEmpty()) {
            query.addCriteria(Criteria.where("detallesContratista.disponibilidad").is(filtro.getDisponibilidad()));
        }
        
        // Filtro por verificación (Punto 13 + Punto 14)
        if (filtro.getVerificado() != null) {
            query.addCriteria(Criteria.where("verificado").is(filtro.getVerificado()));
        }
        
        // Filtro por estado activo (Punto 13)
        if (filtro.getActivo() != null) {
            query.addCriteria(Criteria.where("activo").is(filtro.getActivo()));
        } else {
            // Por defecto, solo activos
            query.addCriteria(Criteria.where("activo").is(true));
        }
        
        // Ordenamiento optimizado
        if (filtro.getOrdenarPor() != null && !filtro.getOrdenarPor().isEmpty()) {
            String orden = filtro.getOrdenAscDesc() != null && filtro.getOrdenAscDesc().equals("DESC") 
                ? "DESC" : "ASC";
            
            Sort sortOrder = orden.equals("DESC") 
                ? Sort.by(Sort.Direction.DESC, filtro.getOrdenarPor())
                : Sort.by(Sort.Direction.ASC, filtro.getOrdenarPor());
            
            query.with(sortOrder);
        } else {
            // Ordenamiento por defecto: más recientes primero
            Sort sortOrder = Sort.by(Sort.Direction.DESC, "creado");
            query.with(sortOrder);
        }
        
        return query;
    }
}

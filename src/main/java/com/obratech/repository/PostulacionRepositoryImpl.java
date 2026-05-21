package com.obratech.repository;

import com.obratech.dto.FiltroPostulacionDTO;
import com.obratech.entity.Postulacion;
import com.obratech.entity.enums.EstadoPostulacion;
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
public class PostulacionRepositoryImpl implements PostulacionRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public PostulacionRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Postulacion> buscarConFiltros(FiltroPostulacionDTO filtro) {
        Query query = construirQuery(filtro);
        @SuppressWarnings("null")
        List<Postulacion> result = mongoTemplate.find(query, Postulacion.class);
        return result;
    }

    @Override
    public Page<Postulacion> buscarConFiltrosPaginado(FiltroPostulacionDTO filtro, Pageable pageable) {
        Query query = construirQuery(filtro);
        
        @SuppressWarnings("null")
        long total = mongoTemplate.count(query, Postulacion.class);
        
        @SuppressWarnings("null")
        Pageable safePageable = pageable != null ? pageable : org.springframework.data.domain.PageRequest.of(0, 20);
        query.with(safePageable);
        @SuppressWarnings("null")
        List<Postulacion> postulaciones = mongoTemplate.find(query, Postulacion.class);
        
        return new PageImpl<>(postulaciones, safePageable, total);
    }

    @Override
    public List<Postulacion> buscarPorEstado(String estado) {
        Query query = new Query();
        try {
            EstadoPostulacion estadoEnum = EstadoPostulacion.valueOf(estado);
            query.addCriteria(Criteria.where("estado").is(estadoEnum));
        } catch (IllegalArgumentException e) {
            // Estado inválido, devuelve lista vacía
            return List.of();
        }
        return mongoTemplate.find(query, Postulacion.class);
    }

    @Override
    public List<Postulacion> buscarPendientes() {
        Query query = new Query();
        query.addCriteria(Criteria.where("estado").is(EstadoPostulacion.PENDING));
        Sort sortOrder = Sort.by(Sort.Direction.DESC, "fechaPostulacion");
        query.with(sortOrder);
        @SuppressWarnings("null")
        List<Postulacion> result = mongoTemplate.find(query, Postulacion.class);
        return result;
    }

    @Override
    public List<Postulacion> buscarAceptadas() {
        Query query = new Query();
        query.addCriteria(Criteria.where("estado").is(EstadoPostulacion.ACCEPTED));
        Sort sortOrder = Sort.by(Sort.Direction.DESC, "fechaPostulacion");
        query.with(sortOrder);
        @SuppressWarnings("null")
        List<Postulacion> result = mongoTemplate.find(query, Postulacion.class);
        return result;
    }

    @Override
    public List<Postulacion> buscarRechazadas() {
        Query query = new Query();
        query.addCriteria(Criteria.where("estado").is(EstadoPostulacion.REJECTED));
        Sort sortOrder = Sort.by(Sort.Direction.DESC, "fechaPostulacion");
        query.with(sortOrder);
        @SuppressWarnings("null")
        List<Postulacion> result = mongoTemplate.find(query, Postulacion.class);
        return result;
    }

    /**
     * Construye la query de MongoDB basada en los criterios del filtro
     */
    private Query construirQuery(FiltroPostulacionDTO filtro) {
        Query query = new Query();
        
        // Filtro por estado
        if (filtro.getEstado() != null && !filtro.getEstado().isEmpty()) {
            try {
                EstadoPostulacion estado = EstadoPostulacion.valueOf(filtro.getEstado());
                query.addCriteria(Criteria.where("estado").is(estado));
            } catch (IllegalArgumentException e) {
                // Estado inválido, se ignora
            }
        }
        
        // Filtro por proyecto
        if (filtro.getProyectoId() != null && !filtro.getProyectoId().isEmpty()) {
            query.addCriteria(Criteria.where("proyecto.$id").is(filtro.getProyectoId()));
        }
        
        // Filtro por usuario
        if (filtro.getUsuarioId() != null && !filtro.getUsuarioId().isEmpty()) {
            query.addCriteria(Criteria.where("usuario.$id").is(filtro.getUsuarioId()));
        }
        
        // Ordenamiento
        if (filtro.getOrdenarPor() != null && !filtro.getOrdenarPor().isEmpty()) {
            String orden = filtro.getOrdenAscDesc() != null && filtro.getOrdenAscDesc().equals("DESC") 
                ? "DESC" : "ASC";
            
            if (orden.equals("DESC")) {
                Sort sortOrder = Sort.by(Sort.Direction.DESC, filtro.getOrdenarPor());
                query.with(sortOrder);
            } else {
                Sort sortOrder = Sort.by(Sort.Direction.ASC, filtro.getOrdenarPor());
                query.with(sortOrder);
            }
        } else {
            // Ordenamiento por defecto: más reciente primero
            Sort sortOrder = Sort.by(Sort.Direction.DESC, "fechaPostulacion");
            query.with(sortOrder);
        }
        
        return query;
    }
}

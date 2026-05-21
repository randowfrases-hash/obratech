package com.obratech.repository;

import com.obratech.dto.FiltroProyectoDTO;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoEjecucion;
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
public class ProyectoRepositoryImpl implements ProyectoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public ProyectoRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Proyecto> buscarConFiltros(FiltroProyectoDTO filtro) {
        Query query = construirQuery(filtro);
        return mongoTemplate.find(query, Proyecto.class);
    }

    @Override
    public Page<Proyecto> buscarConFiltrosPaginado(FiltroProyectoDTO filtro, Pageable pageable) {
        Query query = construirQuery(filtro);
        
        long total = mongoTemplate.count(query, Proyecto.class);
        
        query.with(pageable);
        List<Proyecto> proyectos = mongoTemplate.find(query, Proyecto.class);
        
        return new PageImpl<>(proyectos, pageable, total);
    }

    @Override
    public List<Proyecto> buscarPorEstado(String estado) {
        FiltroProyectoDTO filtro = new FiltroProyectoDTO();
        filtro.setEstado(estado);
        return buscarConFiltros(filtro);
    }

    @Override
    public List<Proyecto> buscarPorUbicacion(String ubicacion) {
        FiltroProyectoDTO filtro = new FiltroProyectoDTO();
        filtro.setUbicacion(ubicacion);
        return buscarConFiltros(filtro);
    }

    @Override
    public List<Proyecto> buscarPorPresupuesto(Double presupuestoMin, Double presupuestoMax) {
        FiltroProyectoDTO filtro = new FiltroProyectoDTO();
        filtro.setPresupuestoMin(presupuestoMin);
        filtro.setPresupuestoMax(presupuestoMax);
        return buscarConFiltros(filtro);
    }

    /**
     * Construye la query de MongoDB basada en los criterios del filtro
     * Punto 13: Filtros avanzados optimizados para gran volumen de datos
     */
    private Query construirQuery(FiltroProyectoDTO filtro) {
        Query query = new Query();
        
        // Filtro por estado de ejecución
        if (filtro.getEstadoEjecucion() != null && !filtro.getEstadoEjecucion().isEmpty()) {
            try {
                EstadoEjecucion estado = EstadoEjecucion.valueOf(filtro.getEstadoEjecucion());
                query.addCriteria(Criteria.where("estadoEjecucion").is(estado));
            } catch (IllegalArgumentException e) {
                // Si el estado no es válido, se ignora
            }
        }
        
        // Filtro por estado de asignación
        if (filtro.getEstadoAsignacion() != null && !filtro.getEstadoAsignacion().isEmpty()) {
            query.addCriteria(Criteria.where("estadoAsignacion").is(filtro.getEstadoAsignacion()));
        }
        
        // Filtro por estado (alternativo)
        if (filtro.getEstado() != null && !filtro.getEstado().isEmpty()) {
            try {
                EstadoEjecucion estado = EstadoEjecucion.valueOf(filtro.getEstado());
                query.addCriteria(Criteria.where("estadoEjecucion").is(estado));
            } catch (IllegalArgumentException e) {
                // Si el estado no es válido, se ignora
            }
        }
        
        // Filtro por categoría (tipoProyecto)
        if (filtro.getCategoria() != null && !filtro.getCategoria().isEmpty()) {
            query.addCriteria(Criteria.where("tipoProyecto").regex(filtro.getCategoria(), "i"));
        }
        
        // Filtro por ubicación
        if (filtro.getUbicacion() != null && !filtro.getUbicacion().isEmpty()) {
            query.addCriteria(Criteria.where("ubicacion").regex(filtro.getUbicacion(), "i"));
        }
        
        // Filtro por rango de presupuesto
        if (filtro.getPresupuestoMin() != null || filtro.getPresupuestoMax() != null) {
            Criteria presupuestoCriteria = Criteria.where("presupuesto");
            
            if (filtro.getPresupuestoMin() != null) {
                presupuestoCriteria = presupuestoCriteria.gte(filtro.getPresupuestoMin());
            }
            
            if (filtro.getPresupuestoMax() != null) {
                presupuestoCriteria = presupuestoCriteria.lte(filtro.getPresupuestoMax());
            }
            
            query.addCriteria(presupuestoCriteria);
        }
        
        // Filtro por rango de fecha
        if (filtro.getFechaInicio() != null || filtro.getFechaFin() != null) {
            Criteria fechaCriteria = Criteria.where("fechaCreacion");
            
            if (filtro.getFechaInicio() != null) {
                fechaCriteria = fechaCriteria.gte(filtro.getFechaInicio().atStartOfDay());
            }
            
            if (filtro.getFechaFin() != null) {
                fechaCriteria = fechaCriteria.lte(filtro.getFechaFin().atTime(23, 59, 59));
            }
            
            query.addCriteria(fechaCriteria);
        }
        
        // Filtro por estado activo/finalizado (Punto 13)
        if (filtro.getActivos() != null) {
            if (filtro.getActivos()) {
                // Proyectos activos: PENDIENTE o EN_EJECUCION
                query.addCriteria(Criteria.where("estadoEjecucion").in(
                    EstadoEjecucion.PENDIENTE,
                    EstadoEjecucion.EN_PROGRESO
                ));
            } else {
                // Proyectos finalizados: COMPLETADO o CANCELADO
                query.addCriteria(Criteria.where("estadoEjecucion").in(
                    EstadoEjecucion.COMPLETADO,
                    EstadoEjecucion.CANCELADO
                ));
            }
        }
        
        // Filtro por cliente verificado (Punto 13 + Punto 14)
        if (filtro.getVerificado() != null && filtro.getVerificado()) {
            query.addCriteria(Criteria.where("cliente.verificado").is(true));
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
            // Ordenamiento por defecto: más reciente primero
            Sort sortOrder = Sort.by(Sort.Direction.DESC, "fechaCreacion");
            query.with(sortOrder);
        }
        
        return query;
    }
    
    @Override
    public List<Proyecto> findByClienteUsernameCustom(String clienteUsername) {
        if (clienteUsername == null || clienteUsername.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // Paso 1: Buscar el usuario por username
        Query userQuery = new Query();
        userQuery.addCriteria(Criteria.where("username").is(clienteUsername));
        com.obratech.entity.Usuario usuario = mongoTemplate.findOne(userQuery, com.obratech.entity.Usuario.class);
        
        if (usuario == null || usuario.getId() == null) {
            return new java.util.ArrayList<>();
        }
        
        // Paso 2: Buscar proyectos donde cliente._id coincida con el ID del usuario encontrado
        Query proyectoQuery = new Query();
        proyectoQuery.addCriteria(Criteria.where("cliente._id").is(usuario.getId()));
        
        return mongoTemplate.find(proyectoQuery, Proyecto.class);
    }
}

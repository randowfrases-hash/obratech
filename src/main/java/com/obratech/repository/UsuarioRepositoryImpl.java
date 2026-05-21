package com.obratech.repository;

import com.obratech.dto.FiltroUsuarioDTO;
import com.obratech.entity.Usuario;
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
public class UsuarioRepositoryImpl implements UsuarioRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public UsuarioRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Usuario> buscarConFiltros(FiltroUsuarioDTO filtro) {
        Query query = construirQuery(filtro);
        @SuppressWarnings("null")
        List<Usuario> result = mongoTemplate.find(query, Usuario.class);
        return result;
    }

    @Override
    public Page<Usuario> buscarConFiltrosPaginado(FiltroUsuarioDTO filtro, Pageable pageable) {
        Query query = construirQuery(filtro);
        
        @SuppressWarnings("null")
        long total = mongoTemplate.count(query, Usuario.class);
        
        @SuppressWarnings("null")
        Pageable safePageable = pageable != null ? pageable : org.springframework.data.domain.PageRequest.of(0, 20);
        query.with(safePageable);
        @SuppressWarnings("null")
        List<Usuario> usuarios = mongoTemplate.find(query, Usuario.class);
        
        return new PageImpl<>(usuarios, safePageable, total);
    }

    @Override
    public List<Usuario> buscarPorRol(String rol) {
        Query query = new Query();
        query.addCriteria(Criteria.where("roles").in(rol));
        @SuppressWarnings("null")
        List<Usuario> result = mongoTemplate.find(query, Usuario.class);
        return result;
    }

    @Override
    public List<Usuario> buscarVerificados() {
        Query query = new Query();
        query.addCriteria(Criteria.where("verificado").is(true));
        @SuppressWarnings("null")
        List<Usuario> result = mongoTemplate.find(query, Usuario.class);
        return result;
    }

    @Override
    public List<Usuario> buscarActivos() {
        Query query = new Query();
        query.addCriteria(Criteria.where("activo").is(true));
        @SuppressWarnings("null")
        List<Usuario> result = mongoTemplate.find(query, Usuario.class);
        return result;
    }

    /**
     * Construye la query de MongoDB basada en los criterios del filtro
     */
    private Query construirQuery(FiltroUsuarioDTO filtro) {
        Query query = new Query();
        
        // Filtro por rol
        if (filtro.getRol() != null && !filtro.getRol().isEmpty()) {
            query.addCriteria(Criteria.where("roles").in(filtro.getRol()));
        }
        
        // Filtro por verificación
        if (filtro.getVerificado() != null) {
            query.addCriteria(Criteria.where("verificado").is(filtro.getVerificado()));
        }
        
        // Filtro por estado activo
        if (filtro.getActivo() != null) {
            query.addCriteria(Criteria.where("activo").is(filtro.getActivo()));
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
            // Ordenamiento por defecto: creado descendente
            Sort sortOrder = Sort.by(Sort.Direction.DESC, "creado");
            query.with(sortOrder);
        }
        
        return query;
    }
}

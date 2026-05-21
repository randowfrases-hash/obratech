package com.obratech.repository;

import com.obratech.entity.Perfil;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio unificado para la coleccion `perfiles`.
 *
 * IMPORTANTE sobre el campo `roles`:
 *   - En la entidad Perfil, `roles` es un Set<String>.
 *   - En MongoDB se almacena como un array: ["ROLE_CLIENT"].
 *   - Para buscar dentro de ese array hay que usar @Query con { roles: valor }
 *     o usar el método derived findByRoles que Spring Data convierte a $in.
 *   - Los métodos con @Query son mas seguros y explícitos para evitar bugs.
 */
public interface PerfilRepository extends MongoRepository<Perfil, String>, PerfilRepositoryCustom {

    // ── Búsqueda por username (email) ──────────────────────────────────────────
    Optional<Perfil> findByUsername(String username);
    Optional<Perfil> findByUsernameIgnoreCase(String username);

    // ── Búsqueda por email ─────────────────────────────────────────────────────
    Optional<Perfil> findByEmailIgnoreCase(String email);

    // ── Filtrar por rol (campo `roles` es un array en MongoDB) ─────────────────
    // @Query explícito: busca documentos donde el array `roles` contenga el valor dado.
    // Esto es más fiable que el método derivado para campos de tipo Set/List.

    /**
     * Retorna todos los perfiles que tengan el rol indicado (activos o no).
     * Equivalente MongoDB: { roles: "ROLE_CONTRACTOR" }
     */
    @Query("{ 'roles': ?0 }")
    List<Perfil> findByRoles(String rol);

    /**
     * Retorna perfiles con el rol indicado Y que estn activos.
     * Es flexible: incluye activo=true y activo=null/missing (asumiendo activo por defecto).
     * Equivalente MongoDB: { roles: "ROLE_CONTRACTOR", activo: { $ne: false } }
     */
    @Query("{ 'roles': ?0, 'activo': { $ne: false } }")
    List<Perfil> findByRolesAndActivoTrue(String rol);

    /**
     * Retorna perfiles con el rol indicado Y que NO estén verificados.
     * Incluye documentos donde verificado es null o false.
     * Equivalente MongoDB: { roles: "ROLE_CONTRACTOR", $or: [{verificado: false}, {verificado: null}] }
     */
    @Query("{ 'roles': ?0, $or: [{ 'verificado': false }, { 'verificado': { $exists: false } }] }")
    List<Perfil> findByRolesAndVerificadoFalse(String rol);

    // ── Búsqueda de contratistas por especialidad ──────────────────────────────
    /**
     * Busca contratistas cuya especialidad contenga el texto dado (case-insensitive).
     * Se usa regex de MongoDB para la búsqueda parcial.
     */
    @Query("{ 'detallesContratista.especialidad': { $regex: ?0, $options: 'i' }, 'roles': ?1 }")
    List<Perfil> findByEspecialidadContainingIgnoreCaseAndRoles(String especialidad, String rol);

    /**
     * Retorna trabajadores marcados con disponibilidad = true y el rol dado.
     * Usa dot-notation para acceder al objeto DetallesTrabajador.
     */
    @Query("{ $or: [ { 'detallesTrabajador.disponibilidad': true }, { 'detallesTrabajador.disponibilidad': { $exists: false } } ], 'roles': ?0 }")
    List<Perfil> findByDisponibilidadTrueAndRoles(String rol);

    /**
     * Retorna perfiles con el rol indicado Y que estén inactivos (activo = false).
     */
    @Query("{ 'roles': ?0, 'activo': false }")
    List<Perfil> findByRolesAndActivoFalse(String rol);

    // ── Conteo ─────────────────────────────────────────────────────────────────
    /**
     * Cuenta perfiles con el rol dado que estén inactivos (activo = false o null).
     */
    @Query(value = "{ 'roles': ?0, $or: [{ 'activo': false }, { 'activo': { $exists: false } }] }",
           count = true)
    long countByRolesAndActivoFalse(String rol);
}

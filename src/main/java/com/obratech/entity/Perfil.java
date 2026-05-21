package com.obratech.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entidad unificada para TODOS los tipos de persona del sistema.
 * Reemplaza las antiguas clases Cliente, Contratista y Trabajador.
 * El campo `role` determina el tipo: ROLE_CLIENT, ROLE_CONTRACTOR, ROLE_WORKER.
 * Todos los documentos se guardan en la coleccin `perfiles`.
 */
@Document(collection = "perfiles")
public class Perfil {

    @Id
    private String id;

    //  Campos comunes a todos los roles 
    private String nombre;
    private String apellido;

    @Indexed
    private String username;   // email del usuario (clave de unin con `usuarios`)

    private String email;      // puede ser igual a username en algunos roles

    private String telefono;

    private java.util.Set<String> roles = new java.util.HashSet<>();       // ROLE_CLIENT | ROLE_CONTRACTOR | ROLE_WORKER

    private Boolean activo = true;
    private Boolean verificado = false;

    private LocalDateTime creado;

    //  Objetos de Detalles por Rol 
    private DetallesCliente detallesCliente;
    private DetallesContratista detallesContratista;
    private DetallesTrabajador detallesTrabajador;

    // 

    public Perfil() {
        if (this.creado == null) this.creado = LocalDateTime.now();
    }

    public Perfil(String nombre, String username) {
        this.nombre = nombre;
        this.username = username;
        this.creado = LocalDateTime.now();
    }

    //  Getters y Setters Bsicos 

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public java.util.Set<String> getRoles() { return roles; }
    public void setRoles(java.util.Set<String> roles) { this.roles = roles; }

    public String getRole() { 
        if (roles == null || roles.isEmpty()) return null;
        if (roles.contains("ROLE_ADMIN")) return "ROLE_ADMIN";
        if (roles.contains("ROLE_CLIENT")) return "ROLE_CLIENT";
        if (roles.contains("ROLE_CONTRACTOR")) return "ROLE_CONTRACTOR";
        if (roles.contains("ROLE_WORKER")) return "ROLE_WORKER";
        return roles.iterator().next(); 
    }
    public void setRole(String role) { 
        if (this.roles == null) this.roles = new java.util.HashSet<>();
        this.roles.add(role); 
    }

    public Boolean getActivo() { return activo != null && activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Boolean getVerificado() { return verificado; }
    public void setVerificado(Boolean verificado) { this.verificado = verificado; }

    public LocalDateTime getCreado() { return creado; }
    public void setCreado(LocalDateTime creado) { this.creado = creado; }

    //  Getters y Setters de Objetos de Detalles 

    public DetallesCliente getDetallesCliente() { return detallesCliente; }
    public void setDetallesCliente(DetallesCliente detallesCliente) { this.detallesCliente = detallesCliente; }

    public DetallesContratista getDetallesContratista() { return detallesContratista; }
    public void setDetallesContratista(DetallesContratista detallesContratista) { this.detallesContratista = detallesContratista; }

    public DetallesTrabajador getDetallesTrabajador() { return detallesTrabajador; }
    public void setDetallesTrabajador(DetallesTrabajador detallesTrabajador) { this.detallesTrabajador = detallesTrabajador; }

    //  Mtodos de Conveniencia (Delegacin) para mantener compatibilidad 

    // Cliente
    public String getEmpresa() { 
        return detallesCliente != null ? detallesCliente.getEmpresa() : null; 
    }
    public void setEmpresa(String empresa) { 
        if (detallesCliente == null) detallesCliente = new DetallesCliente();
        detallesCliente.setEmpresa(empresa); 
    }

    // Contratista
    public String getEspecialidad() { 
        return detallesContratista != null ? detallesContratista.getEspecialidad() : null; 
    }
    public void setEspecialidad(String especialidad) { 
        if (detallesContratista == null) detallesContratista = new DetallesContratista();
        detallesContratista.setEspecialidad(especialidad); 
    }

    public String getUbicacion() { 
        return detallesContratista != null ? detallesContratista.getUbicacion() : null; 
    }
    public void setUbicacion(String ubicacion) { 
        if (detallesContratista == null) detallesContratista = new DetallesContratista();
        detallesContratista.setUbicacion(ubicacion); 
    }

    public String getDescripcion() { 
        return detallesContratista != null ? detallesContratista.getDescripcion() : null; 
    }
    public void setDescripcion(String descripcion) { 
        if (detallesContratista == null) detallesContratista = new DetallesContratista();
        detallesContratista.setDescripcion(descripcion); 
    }

    public Integer getExperiencia() { 
        return detallesContratista != null ? detallesContratista.getExperiencia() : null; 
    }
    public void setExperiencia(Integer experiencia) { 
        if (detallesContratista == null) detallesContratista = new DetallesContratista();
        detallesContratista.setExperiencia(experiencia); 
    }

    public Double getCalificacionPromedio() { 
        return detallesContratista != null ? detallesContratista.getCalificacionPromedio() : null; 
    }
    public void setCalificacionPromedio(Double calificacionPromedio) { 
        if (detallesContratista == null) detallesContratista = new DetallesContratista();
        detallesContratista.setCalificacionPromedio(calificacionPromedio); 
    }

    public String getCvUrl() { 
        return detallesContratista != null ? detallesContratista.getCvUrl() : null; 
    }
    public void setCvUrl(String cvUrl) { 
        if (detallesContratista == null) detallesContratista = new DetallesContratista();
        detallesContratista.setCvUrl(cvUrl); 
    }

    // Trabajador
    public String getOficio() { 
        return detallesTrabajador != null ? detallesTrabajador.getOficio() : null; 
    }
    public void setOficio(String oficio) { 
        if (detallesTrabajador == null) detallesTrabajador = new DetallesTrabajador();
        detallesTrabajador.setOficio(oficio); 
    }

    public Boolean getDisponibilidad() { 
        return detallesTrabajador != null && Boolean.TRUE.equals(detallesTrabajador.getDisponibilidad()); 
    }
    public void setDisponibilidad(Boolean disponibilidad) { 
        if (detallesTrabajador == null) detallesTrabajador = new DetallesTrabajador();
        detallesTrabajador.setDisponibilidad(disponibilidad); 
    }

    @Override
    public String toString() {
        return "Perfil{id=" + id + ", username='" + username + "', role='" + getRole() + "', activo=" + activo + "}";
    }
}

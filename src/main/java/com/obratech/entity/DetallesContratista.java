package com.obratech.entity;

import java.util.HashSet;
import java.util.Set;
import org.springframework.data.mongodb.core.mapping.DBRef;

public class DetallesContratista {

    private String especialidad;
    private String ubicacion;
    private String descripcion;
    private Integer experiencia;
    private Double calificacionPromedio;
    private String cvUrl;

    @DBRef(lazy = true)
    private Set<Proyecto> proyectos = new HashSet<>();

    public DetallesContratista() {}

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getExperiencia() { return experiencia; }
    public void setExperiencia(Integer experiencia) { this.experiencia = experiencia; }

    public Double getCalificacionPromedio() { return calificacionPromedio; }
    public void setCalificacionPromedio(Double calificacionPromedio) { this.calificacionPromedio = calificacionPromedio; }

    public String getCvUrl() { return cvUrl; }
    public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }

    public Set<Proyecto> getProyectos() { return proyectos; }
    public void setProyectos(Set<Proyecto> proyectos) { this.proyectos = proyectos; }
}

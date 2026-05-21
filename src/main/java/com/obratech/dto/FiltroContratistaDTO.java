package com.obratech.dto;

/**
 * DTO para filtros avanzados de búsqueda de contratistas
 * Punto 13: Implementación de filtros avanzados para más de 15 mil registros
 */
public class FiltroContratistaDTO {
    
    private String especialidad; // Ej: "Electricista", "Carpintero", etc.
    private String experiencia; // "Junior", "Intermedio", "Senior"
    private Double calificacionMin; // Ej: 4.0
    private Double calificacionMax; // Ej: 5.0
    private String disponibilidad; // "Disponible", "No disponible"
    private Boolean verificado; // Solo contratistas verificados
    private Boolean activo; // Contratistas activos
    private String ordenarPor; // nombre, calificacion, fechaCreacion, experiencia
    private String ordenAscDesc; // ASC, DESC
    private int pagina = 0;
    private int tamaño = 20;
    
    public FiltroContratistaDTO() {}

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public String getExperiencia() { return experiencia; }
    public void setExperiencia(String experiencia) { this.experiencia = experiencia; }

    public Double getCalificacionMin() { return calificacionMin; }
    public void setCalificacionMin(Double calificacionMin) { this.calificacionMin = calificacionMin; }

    public Double getCalificacionMax() { return calificacionMax; }
    public void setCalificacionMax(Double calificacionMax) { this.calificacionMax = calificacionMax; }

    public String getDisponibilidad() { return disponibilidad; }
    public void setDisponibilidad(String disponibilidad) { this.disponibilidad = disponibilidad; }

    public Boolean getVerificado() { return verificado; }
    public void setVerificado(Boolean verificado) { this.verificado = verificado; }

    public String getOrdenarPor() { return ordenarPor; }
    public void setOrdenarPor(String ordenarPor) { this.ordenarPor = ordenarPor; }

    public String getOrdenAscDesc() { return ordenAscDesc; }
    public void setOrdenAscDesc(String ordenAscDesc) { this.ordenAscDesc = ordenAscDesc; }

    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }

    public int getTamaño() { return tamaño; }
    public void setTamaño(int tamaño) { this.tamaño = tamaño; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}

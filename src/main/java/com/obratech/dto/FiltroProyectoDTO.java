package com.obratech.dto;

import java.time.LocalDate;

/**
 * DTO para filtros avanzados de búsqueda de proyectos
 * Soporta filtrado por múltiples criterios de forma simultánea
 * Punto 13: Implementación de filtros avanzados para más de 15 mil registros
 */
public class FiltroProyectoDTO {
    
    private String estado; // PENDIENTE, EN_PROCESO, FINALIZADO
    private String estadoEjecucion; // PENDIENTE, EN_EJECUCION, COMPLETADO, CANCELADO
    private String estadoAsignacion; // SIN_ASIGNAR, ASIGNADO
    private String categoria; // tipoProyecto
    private String ubicacion;
    private Double presupuestoMin;
    private Double presupuestoMax;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean activos; // true = proyectos activos, false = finalizados
    private Boolean verificado; // Solo proyectos de clientes verificados
    private String ordenarPor; // titulo, presupuesto, fechaCreacion, ubicacion
    private String ordenAscDesc; // ASC, DESC
    private int pagina = 0;
    private int tamaño = 20;
    
    public FiltroProyectoDTO() {}
    
    public FiltroProyectoDTO(String estado, String categoria, String ubicacion) {
        this.estado = estado;
        this.categoria = categoria;
        this.ubicacion = ubicacion;
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public Double getPresupuestoMin() { return presupuestoMin; }
    public void setPresupuestoMin(Double presupuestoMin) { this.presupuestoMin = presupuestoMin; }

    public Double getPresupuestoMax() { return presupuestoMax; }
    public void setPresupuestoMax(Double presupuestoMax) { this.presupuestoMax = presupuestoMax; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getOrdenarPor() { return ordenarPor; }
    public void setOrdenarPor(String ordenarPor) { this.ordenarPor = ordenarPor; }

    public String getOrdenAscDesc() { return ordenAscDesc; }
    public void setOrdenAscDesc(String ordenAscDesc) { this.ordenAscDesc = ordenAscDesc; }

    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }


    public String getEstadoEjecucion() { return estadoEjecucion; }
    public void setEstadoEjecucion(String estadoEjecucion) { this.estadoEjecucion = estadoEjecucion; }

    public String getEstadoAsignacion() { return estadoAsignacion; }
    public void setEstadoAsignacion(String estadoAsignacion) { this.estadoAsignacion = estadoAsignacion; }

    public Boolean getActivos() { return activos; }
    public void setActivos(Boolean activos) { this.activos = activos; }

    public Boolean getVerificado() { return verificado; }
    public void setVerificado(Boolean verificado) { this.verificado = verificado; }

    public int getTamaño() { return tamaño; }
    public void setTamaño(int tamaño) { this.tamaño = tamaño; }
}

package com.obratech.dto;

/**
 * DTO para filtros avanzados de búsqueda de postulaciones
 */
public class FiltroPostulacionDTO {
    
    private String estado; // PENDING, ACCEPTED, REJECTED
    private String proyectoId; // Filtrar por proyecto específico
    private String usuarioId; // Filtrar por usuario específico
    private String ordenarPor; // fechaPostulacion, estado
    private String ordenAscDesc; // ASC, DESC
    private int pagina = 0;
    private int tamaño = 20;
    
    public FiltroPostulacionDTO() {}

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getOrdenarPor() { return ordenarPor; }
    public void setOrdenarPor(String ordenarPor) { this.ordenarPor = ordenarPor; }

    public String getOrdenAscDesc() { return ordenAscDesc; }
    public void setOrdenAscDesc(String ordenAscDesc) { this.ordenAscDesc = ordenAscDesc; }

    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }

    public int getTamaño() { return tamaño; }
    public void setTamaño(int tamaño) { this.tamaño = tamaño; }
}

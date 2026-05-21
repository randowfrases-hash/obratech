package com.obratech.dto;

/**
 * DTO para filtros avanzados de búsqueda de usuarios
 */
public class FiltroUsuarioDTO {
    
    private String rol; // ROLE_CLIENT, ROLE_CONTRACTOR, ROLE_WORKER, ROLE_ADMIN
    private Boolean verificado; // true: verificados, false: no verificados, null: todos
    private Boolean activo; // true: activos, false: inactivos, null: todos
    private String ordenarPor; // username, creado, ultimoAcceso
    private String ordenAscDesc; // ASC, DESC
    private int pagina = 0;
    private int tamaño = 20;
    
    public FiltroUsuarioDTO() {}

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getVerificado() { return verificado; }
    public void setVerificado(Boolean verificado) { this.verificado = verificado; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getOrdenarPor() { return ordenarPor; }
    public void setOrdenarPor(String ordenarPor) { this.ordenarPor = ordenarPor; }

    public String getOrdenAscDesc() { return ordenAscDesc; }
    public void setOrdenAscDesc(String ordenAscDesc) { this.ordenAscDesc = ordenAscDesc; }

    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }

    public int getTamaño() { return tamaño; }
    public void setTamaño(int tamaño) { this.tamaño = tamaño; }
}

package cl.duoc.mesatech.catalogo.dto;

import jakarta.validation.constraints.NotBlank;

public class PrioridadDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    public PrioridadDTO() {
    }

    public PrioridadDTO(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
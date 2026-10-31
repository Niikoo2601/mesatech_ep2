package cl.duoc.mesatech.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CrearCategoriaPeticion {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres.")
    private String descripcion;

    public CrearCategoriaPeticion() {
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
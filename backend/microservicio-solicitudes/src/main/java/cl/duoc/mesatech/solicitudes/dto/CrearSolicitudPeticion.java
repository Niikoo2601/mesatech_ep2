package cl.duoc.mesatech.solicitudes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CrearSolicitudPeticion {

    @NotBlank(message = "El título es obligatorio.")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres.")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres.")
    private String descripcion;

    @NotBlank(message = "La prioridad es obligatoria.")
    @Size(max = 50, message = "La prioridad no puede superar los 50 caracteres.")
    private String prioridad;

    @NotNull(message = "La categoría es obligatoria.")
    private Long categoriaId;

    // Opcional: Solicitudes toma el nombre desde su copia local (categorias_recibidas).
    @Size(max = 150, message = "El nombre de la categoría no puede superar los 150 caracteres.")
    private String categoriaNombre;

    public CrearSolicitudPeticion() {
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }
}
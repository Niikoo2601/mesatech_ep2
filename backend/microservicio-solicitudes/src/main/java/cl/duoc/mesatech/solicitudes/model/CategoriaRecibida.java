package cl.duoc.mesatech.solicitudes.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Copia local de una categoría del Catálogo, recibida por RabbitMQ.
 * Vive en RDS Solicitudes. La clave primaria es el categoriaId ORIGINAL de Catálogo,
 * para poder relacionar el registro de origen con su copia.
 */
@Entity
@Table(name = "categorias_recibidas")
public class CategoriaRecibida {

    @Id
    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "fecha_evento", nullable = false)
    private Instant fechaEvento;

    @Column(name = "fecha_recepcion", nullable = false)
    private Instant fechaRecepcion;

    public CategoriaRecibida() {
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Instant getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(Instant fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public Instant getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(Instant fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }
}

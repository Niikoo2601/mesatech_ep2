package cl.duoc.mesatech.solicitudes.dto;

import cl.duoc.mesatech.solicitudes.model.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;

public class ActualizarEstadoPeticion {

    @NotNull(message = "El estado es obligatorio.")
    private EstadoSolicitud estado;

    public ActualizarEstadoPeticion() {
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }
}
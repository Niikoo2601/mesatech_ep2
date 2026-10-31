package cl.duoc.mesatech.solicitudes.service;

import cl.duoc.mesatech.solicitudes.dto.CrearSolicitudPeticion;
import cl.duoc.mesatech.solicitudes.model.CategoriaRecibida;
import cl.duoc.mesatech.solicitudes.model.Solicitud;
import cl.duoc.mesatech.solicitudes.repository.SolicitudRepositorio;
import cl.duoc.mesatech.solicitudes.model.EstadoSolicitud;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SolicitudServicio {

    private final SolicitudRepositorio solicitudRepositorio;
    private final CategoriaRecibidaServicio categoriaRecibidaServicio;

    public SolicitudServicio(
            SolicitudRepositorio solicitudRepositorio,
            CategoriaRecibidaServicio categoriaRecibidaServicio) {
        this.solicitudRepositorio = solicitudRepositorio;
        this.categoriaRecibidaServicio = categoriaRecibidaServicio;
    }

    public Solicitud crearSolicitud(
            CrearSolicitudPeticion peticion,
            String usuario) {

        // La categoría debe existir en la COPIA LOCAL (recibida por RabbitMQ).
        // Nunca se consulta la base de datos de Catálogo.
        CategoriaRecibida categoria = categoriaRecibidaServicio
                .buscar(peticion.getCategoriaId())
                .orElseThrow(() -> new CategoriaNoDisponibleException(peticion.getCategoriaId()));

        Solicitud solicitud = new Solicitud();

        solicitud.setTitulo(peticion.getTitulo().trim());
        solicitud.setDescripcion(peticion.getDescripcion().trim());
        solicitud.setCategoriaId(categoria.getCategoriaId());
        solicitud.setCategoriaNombre(categoria.getNombre());
        solicitud.setPrioridad(
                peticion.getPrioridad().trim());
        solicitud.setUsuario(usuario.trim());

        return solicitudRepositorio.save(solicitud);
    }

    public List<Solicitud> listarSolicitudesDelUsuario(String usuario) {
        return solicitudRepositorio
                .findByUsuarioOrderByFechaCreacionDesc(usuario.trim());
    }

    public Optional<Solicitud> buscarSolicitud(
            Long id,
            String usuario) {

        return solicitudRepositorio
                .findByIdAndUsuario(id, usuario.trim());
    }

    public List<Solicitud> listarTodasLasSolicitudes() {
        return solicitudRepositorio
                .findAllByOrderByFechaCreacionDesc();
    }

    public Optional<Solicitud> actualizarEstado(
            Long id,
            EstadoSolicitud nuevoEstado) {

        return solicitudRepositorio.findById(id).map(solicitud -> {
            validarTransicion(
                    solicitud.getEstado(),
                    nuevoEstado);

            solicitud.setEstado(nuevoEstado);
            return solicitudRepositorio.save(solicitud);
        });
    }

    private void validarTransicion(
            EstadoSolicitud estadoActual,
            EstadoSolicitud nuevoEstado) {

        boolean transicionValida = switch (estadoActual) {
            case CREADA ->
                nuevoEstado == EstadoSolicitud.ASIGNADA
                        || nuevoEstado == EstadoSolicitud.CANCELADA;

            case ASIGNADA ->
                nuevoEstado == EstadoSolicitud.EN_PROCESO
                        || nuevoEstado == EstadoSolicitud.CANCELADA;

            case EN_PROCESO ->
                nuevoEstado == EstadoSolicitud.RESUELTA
                        || nuevoEstado == EstadoSolicitud.CANCELADA;

            case RESUELTA ->
                nuevoEstado == EstadoSolicitud.CERRADA;

            case CERRADA, CANCELADA -> false;
        };

        if (!transicionValida) {
            throw new IllegalStateException(
                    "No se puede cambiar el estado de "
                            + estadoActual
                            + " a "
                            + nuevoEstado
                            + ".");
        }
    }
}
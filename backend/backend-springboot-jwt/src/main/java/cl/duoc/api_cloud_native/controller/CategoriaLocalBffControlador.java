package cl.duoc.api_cloud_native.controller;

import cl.duoc.api_cloud_native.service.SolicitudClienteServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Categorías disponibles en Solicitudes (copia local recibida por RabbitMQ).
 * React las usa para el formulario de nueva solicitud y para mostrar
 * si una categoría recién creada ya fue sincronizada.
 */
@RestController
@RequestMapping("/api/categorias-locales")
public class CategoriaLocalBffControlador {

    private final SolicitudClienteServicio solicitudClienteServicio;

    public CategoriaLocalBffControlador(SolicitudClienteServicio solicitudClienteServicio) {
        this.solicitudClienteServicio = solicitudClienteServicio;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENTE', 'ROLE_OPERADOR', 'ROLE_ADMINISTRADOR')")
    public ResponseEntity<Object> listar() {
        return solicitudClienteServicio.listarCategoriasRecibidas();
    }
}

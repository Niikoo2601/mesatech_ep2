package cl.duoc.mesatech.solicitudes.controller;

import cl.duoc.mesatech.solicitudes.model.CategoriaRecibida;
import cl.duoc.mesatech.solicitudes.service.CategoriaRecibidaServicio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Categorías disponibles para registrar solicitudes: es la copia local recibida por RabbitMQ.
 * Se expone al exterior solo a través del BFF (GET /api/categorias-locales, con JWT).
 */
@RestController
@RequestMapping("/api/categorias-recibidas")
public class CategoriaRecibidaControlador {

    private final CategoriaRecibidaServicio servicio;

    public CategoriaRecibidaControlador(CategoriaRecibidaServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<CategoriaRecibida> listar() {
        return servicio.listar();
    }
}

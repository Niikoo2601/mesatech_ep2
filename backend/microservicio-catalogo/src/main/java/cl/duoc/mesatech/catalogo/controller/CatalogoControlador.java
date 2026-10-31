package cl.duoc.mesatech.catalogo.controller;

import cl.duoc.mesatech.catalogo.dto.CrearCategoriaPeticion;
import cl.duoc.mesatech.catalogo.mensajeria.PublicacionEventoException;
import cl.duoc.mesatech.catalogo.model.Categoria;
import cl.duoc.mesatech.catalogo.service.CatalogoServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogo")
@Validated
public class CatalogoControlador {

    private final CatalogoServicio catalogoServicio;

    public CatalogoControlador(CatalogoServicio catalogoServicio) {
        this.catalogoServicio = catalogoServicio;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearCategoria(
            @Valid
            @RequestBody
            CrearCategoriaPeticion peticion) {

        try {
            CatalogoServicio.CategoriaCreada resultado = catalogoServicio.crearCategoria(peticion);
            Categoria categoria = resultado.categoria();

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("id", categoria.getId());
            cuerpo.put("nombre", categoria.getNombre());
            cuerpo.put("descripcion", categoria.getDescripcion());
            cuerpo.put("eventId", resultado.evento().eventId());
            cuerpo.put("publicado", true);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("X-Event-Id", resultado.evento().eventId())
                    .body(cuerpo);

        } catch (PublicacionEventoException error) {
            // La categoría quedó guardada en RDS Catálogo, pero el evento no fue confirmado.
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("mensaje", "La categoría se guardó en Catálogo (id "
                    + error.getEvento().categoriaId()
                    + "), pero RabbitMQ no confirmó la publicación del evento. "
                    + "Solicitudes no la recibirá hasta que el evento se publique nuevamente.");
            cuerpo.put("detalle", error.getMessage());
            cuerpo.put("categoriaId", error.getEvento().categoriaId());
            cuerpo.put("eventId", error.getEvento().eventId());
            cuerpo.put("publicado", false);

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(cuerpo);
        }
    }

    @GetMapping
    public List<Categoria> listarCategorias() {
        return catalogoServicio.listarCategorias();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categoria> buscarCategoria(
            @PathVariable Long id) {

        return catalogoServicio
                .buscarCategoria(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody CrearCategoriaPeticion peticion) {
        
        try {
            Categoria categoriaActualizada = catalogoServicio.actualizarCategoria(id, peticion);
            return ResponseEntity.ok(categoriaActualizada);
        } catch (RuntimeException e) {
            // devolvemos un 404
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long id) {
        try {
            catalogoServicio.eliminarCategoria(id);
            // Devuelve 204
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    
    
    
}
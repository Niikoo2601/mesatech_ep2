package cl.duoc.api_cloud_native.controller;

import cl.duoc.api_cloud_native.service.CatalogoClienteServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoBffControlador {

    private final CatalogoClienteServicio catalogoClienteServicio;

    public CatalogoBffControlador(
            CatalogoClienteServicio catalogoClienteServicio) {

        this.catalogoClienteServicio = catalogoClienteServicio;
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyAuthority('ROLE_CLIENTE', 'ROLE_OPERADOR', 'ROLE_ADMINISTRADOR')")
    public ResponseEntity<Object> listarCategorias() {

        return catalogoClienteServicio.listarCategorias();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<Object> crearCategoria(
            @RequestBody Map<String, Object> peticion) {

        return catalogoClienteServicio.crearCategoria(peticion);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<Object> actualizarCategoria(
            @PathVariable Long id,
            @RequestBody Map<String, Object> peticion) {

        return catalogoClienteServicio
                .actualizarCategoria(id, peticion);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarCategoria(
            @PathVariable Long id) {

        return catalogoClienteServicio.eliminarCategoria(id);
    }
}
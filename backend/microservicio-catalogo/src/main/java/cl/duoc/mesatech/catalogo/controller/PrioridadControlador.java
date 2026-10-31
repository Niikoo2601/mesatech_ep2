package cl.duoc.mesatech.catalogo.controller;

import cl.duoc.mesatech.catalogo.dto.PrioridadDTO;
import cl.duoc.mesatech.catalogo.model.Prioridad;
import cl.duoc.mesatech.catalogo.service.PrioridadServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/prioridades")
public class PrioridadControlador {

    @Autowired
    private PrioridadServicio prioridadServicio;

    @GetMapping
    public ResponseEntity<List<Prioridad>> listar() {
        return ResponseEntity.ok(prioridadServicio.obtenerTodas());
    }

    @PostMapping
    public ResponseEntity<Prioridad> crear(@Valid @RequestBody PrioridadDTO dto) {
        return new ResponseEntity<>(prioridadServicio.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Prioridad> actualizar(@PathVariable Long id, @Valid @RequestBody PrioridadDTO dto) {
        return ResponseEntity.ok(prioridadServicio.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        prioridadServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
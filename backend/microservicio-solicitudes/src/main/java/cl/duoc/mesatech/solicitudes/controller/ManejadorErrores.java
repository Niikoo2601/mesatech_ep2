package cl.duoc.mesatech.solicitudes.controller;

import cl.duoc.mesatech.solicitudes.service.CategoriaNoDisponibleException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(CategoriaNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> categoriaNoDisponible(CategoriaNoDisponibleException error) {
        return ResponseEntity
                .status(422) // Unprocessable Content
                .body(Map.of("mensaje", error.getMessage()));
    }
}

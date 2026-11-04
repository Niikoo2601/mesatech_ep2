package cl.duoc.api_cloud_native.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class ManejadorErroresMicroservicios {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresMicroservicios.class);

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> errorDelMicroservicio(RestClientResponseException error) {
        log.warn("Microservicio respondió {}: {}", error.getStatusCode(), error.getResponseBodyAsString());

        String cuerpo = error.getResponseBodyAsString();
        if (cuerpo == null || cuerpo.isBlank()) {
            cuerpo = "{\"mensaje\":\"El microservicio respondió " + error.getStatusCode().value() + ".\"}";
        }

        return ResponseEntity
                .status(error.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> microservicioNoDisponible(ResourceAccessException error) {
        log.error("Microservicio no disponible: {}", error.getMessage());
        return ResponseEntity
                .status(503)
                .body(Map.of("mensaje", "El microservicio no está disponible en este momento."));
    }
}

package cl.duoc.mesatech.solicitudes.controller;

import cl.duoc.mesatech.solicitudes.dto.CrearSolicitudPeticion;
import cl.duoc.mesatech.solicitudes.model.Solicitud;
import cl.duoc.mesatech.solicitudes.service.SolicitudServicio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import cl.duoc.mesatech.solicitudes.dto.ActualizarEstadoPeticion;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes")
@Validated
public class SolicitudControlador {

        private final SolicitudServicio solicitudServicio;

        public SolicitudControlador(SolicitudServicio solicitudServicio) {
                this.solicitudServicio = solicitudServicio;
        }

        @PostMapping
        public ResponseEntity<Solicitud> crearSolicitud(
                        @RequestHeader("X-Usuario") @NotBlank(message = "El usuario es obligatorio.") String usuario,

                        @Valid @RequestBody CrearSolicitudPeticion peticion) {

                Solicitud solicitudCreada = solicitudServicio.crearSolicitud(peticion, usuario);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(solicitudCreada);
        }

        @GetMapping
        public List<Solicitud> listarSolicitudes(
                        @RequestHeader("X-Usuario") @NotBlank(message = "El usuario es obligatorio.") String usuario) {

                return solicitudServicio
                                .listarSolicitudesDelUsuario(usuario);
        }

        @GetMapping("/{id}")
        public ResponseEntity<Solicitud> buscarSolicitud(
                        @PathVariable Long id,

                        @RequestHeader("X-Usuario") @NotBlank(message = "El usuario es obligatorio.") String usuario) {

                return solicitudServicio
                                .buscarSolicitud(id, usuario)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/todas")
        public List<Solicitud> listarTodasLasSolicitudes() {
                return solicitudServicio.listarTodasLasSolicitudes();
        }

        @PatchMapping("/{id}/estado")
        public ResponseEntity<?> actualizarEstado(
                        @PathVariable Long id,
                        @Valid @RequestBody ActualizarEstadoPeticion peticion) {

                try {
                        return solicitudServicio
                                        .actualizarEstado(id, peticion.getEstado())
                                        .map(ResponseEntity::ok)
                                        .orElseGet(() -> ResponseEntity.notFound().build());

                } catch (IllegalStateException error) {
                        return ResponseEntity
                                        .status(HttpStatus.CONFLICT)
                                        .body(Map.of(
                                                        "mensaje",
                                                        error.getMessage()));
                }
        }
}
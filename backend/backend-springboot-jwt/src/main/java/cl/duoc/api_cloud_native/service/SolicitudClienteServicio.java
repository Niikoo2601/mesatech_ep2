package cl.duoc.api_cloud_native.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class SolicitudClienteServicio {

        private final RestClient clienteSolicitudes;

        public SolicitudClienteServicio(
                        RestClient.Builder constructorCliente,
                        @Value("${microservicios.solicitudes.url}") String urlSolicitudes) {

                this.clienteSolicitudes = constructorCliente
                                .baseUrl(urlSolicitudes)
                                .build();
        }

        public ResponseEntity<Object> crearSolicitud(
                        String usuario,
                        Map<String, Object> peticion) {

                return clienteSolicitudes
                                .post()
                                .uri("/api/solicitudes")
                                .header("X-Usuario", usuario)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(peticion)
                                .retrieve()
                                .toEntity(Object.class);
        }

        public ResponseEntity<Object> listarSolicitudes(String usuario) {

                return clienteSolicitudes
                                .get()
                                .uri("/api/solicitudes")
                                .header("X-Usuario", usuario)
                                .retrieve()
                                .toEntity(Object.class);
        }

        public ResponseEntity<Object> buscarSolicitud(
                        Long id,
                        String usuario) {

                return clienteSolicitudes
                                .get()
                                .uri("/api/solicitudes/{id}", id)
                                .header("X-Usuario", usuario)
                                .retrieve()
                                .toEntity(Object.class);
        }

        public ResponseEntity<Object> listarTodasLasSolicitudes() {

                return clienteSolicitudes
                                .get()
                                .uri("/api/solicitudes/todas")
                                .retrieve()
                                .toEntity(Object.class);
        }

        public ResponseEntity<Object> actualizarEstado(
                        Long id,
                        Map<String, Object> peticion) {

                return clienteSolicitudes
                                .patch()
                                .uri("/api/solicitudes/{id}/estado", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(peticion)
                                .retrieve()
                                .toEntity(Object.class);
        }

        public ResponseEntity<Object> listarCategoriasRecibidas() {

                return clienteSolicitudes
                                .get()
                                .uri("/api/categorias-recibidas")
                                .retrieve()
                                .toEntity(Object.class);
        }
}

package cl.duoc.api_cloud_native.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class CatalogoClienteServicio {

    private final RestClient clienteCatalogo;

    public CatalogoClienteServicio(
            RestClient.Builder constructorCliente,
            @Value("${microservicios.catalogo.url}")
            String urlCatalogo) {

        this.clienteCatalogo = constructorCliente
                .baseUrl(urlCatalogo)
                .build();
    }

    public ResponseEntity<Object> listarCategorias() {

        return clienteCatalogo
                .get()
                .uri("/api/catalogo")
                .retrieve()
                .toEntity(Object.class);
    }

    public ResponseEntity<Object> crearCategoria(
            Map<String, Object> peticion) {

        return clienteCatalogo
                .post()
                .uri("/api/catalogo")
                .contentType(MediaType.APPLICATION_JSON)
                .body(peticion)
                .retrieve()
                .toEntity(Object.class);
    }

    public ResponseEntity<Object> actualizarCategoria(
            Long id,
            Map<String, Object> peticion) {

        return clienteCatalogo
                .put()
                .uri("/api/catalogo/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(peticion)
                .retrieve()
                .toEntity(Object.class);
    }

    public ResponseEntity<Void> eliminarCategoria(Long id) {

        return clienteCatalogo
                .delete()
                .uri("/api/catalogo/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
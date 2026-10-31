package cl.duoc.mesatech.solicitudes.service;

import cl.duoc.mesatech.solicitudes.mensajeria.CategoriaCreadaEvento;
import cl.duoc.mesatech.solicitudes.model.CategoriaRecibida;
import cl.duoc.mesatech.solicitudes.repository.CategoriaRecibidaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CategoriaRecibidaServicio {

    private final CategoriaRecibidaRepositorio repositorio;

    public CategoriaRecibidaServicio(CategoriaRecibidaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Guarda (o reemplaza) la copia local usando el categoriaId de origen como clave.
     * Si RabbitMQ vuelve a entregar el mismo evento, se sobrescribe la misma fila
     * en lugar de duplicarla. Esto NO garantiza procesamiento exactamente una vez.
     */
    @Transactional
    public CategoriaRecibida guardarCopia(CategoriaCreadaEvento evento) {
        CategoriaRecibida copia = repositorio
                .findById(evento.categoriaId())
                .orElseGet(CategoriaRecibida::new);

        copia.setCategoriaId(evento.categoriaId());
        copia.setNombre(evento.nombre().trim());
        copia.setEventId(evento.eventId());
        copia.setFechaEvento(Instant.parse(evento.fechaEvento()));
        copia.setFechaRecepcion(Instant.now());

        return repositorio.saveAndFlush(copia);
    }

    public List<CategoriaRecibida> listar() {
        return repositorio.findAllByOrderByNombreAsc();
    }

    public Optional<CategoriaRecibida> buscar(Long categoriaId) {
        return repositorio.findById(categoriaId);
    }
}

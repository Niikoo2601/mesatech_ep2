package cl.duoc.mesatech.catalogo.service;

import cl.duoc.mesatech.catalogo.dto.CrearCategoriaPeticion;
import cl.duoc.mesatech.catalogo.mensajeria.CategoriaCreadaEvento;
import cl.duoc.mesatech.catalogo.mensajeria.CategoriaEventoPublicador;
import cl.duoc.mesatech.catalogo.model.Categoria;
import cl.duoc.mesatech.catalogo.repository.CategoriaRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CatalogoServicio {

    private static final Logger log = LoggerFactory.getLogger(CatalogoServicio.class);

    private final CategoriaRepositorio categoriaRepositorio;
    private final CategoriaEventoPublicador publicador;

    public CatalogoServicio(
            CategoriaRepositorio categoriaRepositorio,
            CategoriaEventoPublicador publicador) {
        this.categoriaRepositorio = categoriaRepositorio;
        this.publicador = publicador;
    }

    /**
     * 1) Guarda la categoría en RDS Catálogo (save confirma su propia transacción).
     * 2) Publica el evento CATEGORIA_CREADA y espera la confirmación del broker.
     *
     * Son operaciones separadas: si (1) funciona y (2) falla, la categoría queda
     * guardada y se lanza PublicacionEventoException. No hay reversión automática.
     */
    public CategoriaCreada crearCategoria(CrearCategoriaPeticion peticion) {
        Categoria categoria = new Categoria();
        categoria.setNombre(peticion.getNombre().trim());
        categoria.setDescripcion(peticion.getDescripcion().trim());

        Categoria guardada = categoriaRepositorio.save(categoria);
        log.info("Categoría guardada en RDS Catálogo categoriaId={} nombre={}",
                guardada.getId(), guardada.getNombre());

        CategoriaCreadaEvento evento = publicador.publicarCategoriaCreada(guardada);

        return new CategoriaCreada(guardada, evento);
    }

    public record CategoriaCreada(Categoria categoria, CategoriaCreadaEvento evento) {
    }

    public List<Categoria> listarCategorias() {
        return categoriaRepositorio.findAll();
    }

    public Optional<Categoria> buscarCategoria(Long id) {
        return categoriaRepositorio.findById(id);
    }
    
    public Categoria actualizarCategoria(Long id, CrearCategoriaPeticion peticion) {
       
        Categoria categoriaExistente = categoriaRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con el ID: " + id));

       
        categoriaExistente.setNombre(peticion.getNombre());
        categoriaExistente.setDescripcion(peticion.getDescripcion());

        return categoriaRepositorio.save(categoriaExistente);
    }

    //Se clasifica como void ya que su retorno no debe entregar nigun valor 
    public void eliminarCategoria(Long id) {
        
        if (!categoriaRepositorio.existsById(id)) {
            throw new RuntimeException("Categoría no encontrada con el ID: " + id);
        }
        categoriaRepositorio.deleteById(id);
    }
}
package cl.duoc.mesatech.solicitudes.repository;

import cl.duoc.mesatech.solicitudes.model.CategoriaRecibida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRecibidaRepositorio extends JpaRepository<CategoriaRecibida, Long> {

    List<CategoriaRecibida> findAllByOrderByNombreAsc();
}

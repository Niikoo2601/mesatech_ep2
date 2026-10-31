package cl.duoc.mesatech.catalogo.repository;

import cl.duoc.mesatech.catalogo.model.Prioridad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrioridadRepositorio extends JpaRepository<Prioridad, Long> {

}
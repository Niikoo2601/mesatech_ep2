package cl.duoc.mesatech.catalogo.repository;

import cl.duoc.mesatech.catalogo.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {
}
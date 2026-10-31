package cl.duoc.mesatech.solicitudes.repository;

import cl.duoc.mesatech.solicitudes.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolicitudRepositorio extends JpaRepository<Solicitud, Long> {

    List<Solicitud> findByUsuarioOrderByFechaCreacionDesc(String usuario);

    Optional<Solicitud> findByIdAndUsuario(Long id, String usuario);

    List<Solicitud> findAllByOrderByFechaCreacionDesc();
}
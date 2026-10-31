package cl.duoc.mesatech.catalogo.service;

import cl.duoc.mesatech.catalogo.dto.PrioridadDTO;
import cl.duoc.mesatech.catalogo.model.Prioridad;
import cl.duoc.mesatech.catalogo.repository.PrioridadRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrioridadServicio {

    @Autowired
    private PrioridadRepositorio prioridadRepositorio;

    public List<Prioridad> obtenerTodas() {
        return prioridadRepositorio.findAll();
    }

    public Prioridad obtenerPorId(Long id) {
        return prioridadRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Prioridad no encontrada"));
    }

    public Prioridad crear(PrioridadDTO dto) {
        Prioridad prioridad = new Prioridad();
        prioridad.setNombre(dto.getNombre());
        prioridad.setDescripcion(dto.getDescripcion());
        return prioridadRepositorio.save(prioridad);
    }

    public Prioridad actualizar(Long id, PrioridadDTO dto) {
        Prioridad prioridad = obtenerPorId(id);
        prioridad.setNombre(dto.getNombre());
        prioridad.setDescripcion(dto.getDescripcion());
        return prioridadRepositorio.save(prioridad);
    }

    public void eliminar(Long id) {
        prioridadRepositorio.deleteById(id);
    }
}
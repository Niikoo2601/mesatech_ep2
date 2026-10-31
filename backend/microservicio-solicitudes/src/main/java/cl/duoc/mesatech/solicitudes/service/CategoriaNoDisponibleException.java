package cl.duoc.mesatech.solicitudes.service;

/**
 * La categoría todavía no existe en la copia local de Solicitudes
 * (el evento aún no se consume, o nunca se publicó).
 */
public class CategoriaNoDisponibleException extends RuntimeException {

    public CategoriaNoDisponibleException(Long categoriaId) {
        super("La categoría " + categoriaId
                + " todavía no está disponible en Solicitudes. Espere a que se procese el evento e intente nuevamente.");
    }
}

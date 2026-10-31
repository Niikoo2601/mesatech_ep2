package cl.duoc.mesatech.solicitudes.mensajeria;

/**
 * Contrato del evento CATEGORIA_CREADA (copia del lado consumidor).
 * Debe coincidir campo a campo con el publicado por Catálogo.
 * fechaEvento llega como texto ISO-8601 en UTC, por ejemplo "2026-10-30T22:15:03.512Z".
 */
public record CategoriaCreadaEvento(
        String eventId,
        String tipoEvento,
        String fechaEvento,
        Long categoriaId,
        String nombre) {

    public static final String TIPO = "CATEGORIA_CREADA";
}

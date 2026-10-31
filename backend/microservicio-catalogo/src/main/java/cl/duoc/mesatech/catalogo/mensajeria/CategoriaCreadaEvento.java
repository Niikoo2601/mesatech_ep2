package cl.duoc.mesatech.catalogo.mensajeria;

/**
 * Contrato del evento publicado por Catálogo cuando se crea una categoría.
 *
 * Ejemplo de JSON publicado:
 * {
 *   "eventId": "3f1c2a9e-8d7b-4b8e-9a51-0c6d2f1e7a44",
 *   "tipoEvento": "CATEGORIA_CREADA",
 *   "fechaEvento": "2026-10-30T22:15:03.512Z",
 *   "categoriaId": 7,
 *   "nombre": "Redes"
 * }
 *
 * @param eventId     UUID v4 generado por el publicador (también se envía como messageId AMQP).
 * @param tipoEvento  Siempre "CATEGORIA_CREADA" en el alcance mínimo.
 * @param fechaEvento Fecha y hora ISO-8601 en UTC (sufijo Z).
 * @param categoriaId Id de la categoría en RDS Catálogo (clave de origen, no se reemplaza).
 * @param nombre      Nombre de la categoría.
 */
public record CategoriaCreadaEvento(
        String eventId,
        String tipoEvento,
        String fechaEvento,
        Long categoriaId,
        String nombre) {

    public static final String TIPO = "CATEGORIA_CREADA";
}

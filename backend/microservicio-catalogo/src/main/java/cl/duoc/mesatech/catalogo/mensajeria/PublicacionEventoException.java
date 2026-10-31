package cl.duoc.mesatech.catalogo.mensajeria;

/**
 * Se lanza cuando RabbitMQ no confirma (ack) la publicación del evento,
 * cuando el mensaje vuelve sin ruta a una cola (return) o cuando el broker no está disponible.
 *
 * La categoría YA fue guardada en RDS Catálogo antes de intentar publicar:
 * guardar y publicar son operaciones separadas y no hay reversión automática.
 */
public class PublicacionEventoException extends RuntimeException {

    private final transient CategoriaCreadaEvento evento;

    public PublicacionEventoException(CategoriaCreadaEvento evento, String motivo, Throwable causa) {
        super(motivo, causa);
        this.evento = evento;
    }

    public PublicacionEventoException(CategoriaCreadaEvento evento, String motivo) {
        this(evento, motivo, null);
    }

    public CategoriaCreadaEvento getEvento() {
        return evento;
    }
}

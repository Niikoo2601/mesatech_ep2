package cl.duoc.mesatech.catalogo.mensajeria;

import cl.duoc.mesatech.catalogo.model.Categoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publicador del evento CATEGORIA_CREADA.
 *
 * - Publica en el exchange direct durable (catalogo.exchange) con la routing key categoria.creada.
 * - El mensaje es persistente (deliveryMode = 2) y lleva messageId = eventId.
 * - Espera la confirmación del broker (publisher confirm) y revisa si el mensaje
 *   fue devuelto por no tener ruta a una cola (mandatory + publisher returns).
 */
@Component
public class CategoriaEventoPublicador {

    private static final Logger log = LoggerFactory.getLogger(CategoriaEventoPublicador.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final Duration timeoutConfirmacion;

    public CategoriaEventoPublicador(
            RabbitTemplate rabbitTemplate,
            @Value("${mesatech.mensajeria.exchange}") String exchange,
            @Value("${mesatech.mensajeria.routing-key}") String routingKey,
            @Value("${mesatech.mensajeria.timeout-confirmacion:5s}") Duration timeoutConfirmacion) {

        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.timeoutConfirmacion = timeoutConfirmacion;
    }

    public CategoriaCreadaEvento publicarCategoriaCreada(Categoria categoria) {

        CategoriaCreadaEvento evento = new CategoriaCreadaEvento(
                UUID.randomUUID().toString(),
                CategoriaCreadaEvento.TIPO,
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                categoria.getId(),
                categoria.getNombre());

        CorrelationData correlacion = new CorrelationData(evento.eventId());

        log.info("Publicando evento {} eventId={} categoriaId={} exchange={} routingKey={}",
                evento.tipoEvento(), evento.eventId(), evento.categoriaId(), exchange, routingKey);

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, evento, mensaje -> {
                mensaje.getMessageProperties().setMessageId(evento.eventId());
                mensaje.getMessageProperties().setType(evento.tipoEvento());
                mensaje.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return mensaje;
            }, correlacion);

            CorrelationData.Confirm confirmacion = correlacion
                    .getFuture()
                    .get(timeoutConfirmacion.toMillis(), TimeUnit.MILLISECONDS);

            // Spring AMQP garantiza que el "return" se registra antes de completar la confirmación.
            ReturnedMessage devuelto = correlacion.getReturned();
            if (devuelto != null) {
                throw new PublicacionEventoException(evento,
                        "RabbitMQ devolvió el mensaje sin ruta a una cola (replyCode="
                                + devuelto.getReplyCode() + ", replyText=" + devuelto.getReplyText() + ").");
            }

            if (!confirmacion.isAck()) {
                throw new PublicacionEventoException(evento,
                        "RabbitMQ rechazó la publicación (nack): " + confirmacion.getReason());
            }

            log.info("Evento confirmado por RabbitMQ (ack) eventId={} categoriaId={}",
                    evento.eventId(), evento.categoriaId());
            return evento;

        } catch (PublicacionEventoException error) {
            log.error("Publicación NO confirmada eventId={} categoriaId={}: {}",
                    evento.eventId(), evento.categoriaId(), error.getMessage());
            throw error;

        } catch (AmqpException error) {
            log.error("Broker no disponible al publicar eventId={} categoriaId={}: {}",
                    evento.eventId(), evento.categoriaId(), error.getMessage());
            throw new PublicacionEventoException(evento,
                    "No fue posible conectarse con RabbitMQ.", error);

        } catch (TimeoutException error) {
            log.error("Sin confirmación de RabbitMQ en {} eventId={} categoriaId={}",
                    timeoutConfirmacion, evento.eventId(), evento.categoriaId());
            throw new PublicacionEventoException(evento,
                    "RabbitMQ no confirmó la publicación dentro de " + timeoutConfirmacion.toSeconds() + " s.", error);

        } catch (ExecutionException error) {
            throw new PublicacionEventoException(evento,
                    "Error al esperar la confirmación de RabbitMQ.", error);

        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new PublicacionEventoException(evento,
                    "Se interrumpió la espera de la confirmación de RabbitMQ.", error);
        }
    }
}

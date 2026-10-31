package cl.duoc.mesatech.solicitudes.mensajeria;

import cl.duoc.mesatech.solicitudes.model.CategoriaRecibida;
import cl.duoc.mesatech.solicitudes.service.CategoriaRecibidaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Consumidor del evento CATEGORIA_CREADA en la cola solicitudes.catalogo.queue.
 *
 * Modo de confirmación AUTO (spring.rabbitmq.listener.simple.acknowledge-mode=auto):
 * Spring envía el ack SOLO cuando este método termina sin excepción, es decir,
 * después de que la copia quedó guardada en RDS Solicitudes. Si guardar falla,
 * la excepción se propaga, no hay ack y el mensaje vuelve a la cola.
 */
@Component
public class CategoriaCreadaConsumidor {

    private static final Logger log = LoggerFactory.getLogger(CategoriaCreadaConsumidor.class);

    private final CategoriaRecibidaServicio servicio;

    public CategoriaCreadaConsumidor(CategoriaRecibidaServicio servicio) {
        this.servicio = servicio;
    }

    @RabbitListener(queues = "${mesatech.mensajeria.cola}")
    public void recibir(
            CategoriaCreadaEvento evento,
            @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {

        log.info("Evento recibido tipo={} eventId={} messageId={} categoriaId={} nombre={}",
                evento.tipoEvento(), evento.eventId(), messageId, evento.categoriaId(), evento.nombre());

        validar(evento);

        CategoriaRecibida copia = servicio.guardarCopia(evento);

        log.info("Copia guardada en RDS Solicitudes (tabla categorias_recibidas) categoriaId={} eventId={} "
                        + "-> se confirma (ack) el mensaje",
                copia.getCategoriaId(), copia.getEventId());
    }

    /**
     * Un mensaje mal formado nunca podrá guardarse: se rechaza sin reencolar para no
     * bloquear la cola. (No hay DLQ en el alcance de la evaluación.)
     */
    private void validar(CategoriaCreadaEvento evento) {
        if (evento.eventId() == null || evento.categoriaId() == null
                || evento.nombre() == null || evento.nombre().isBlank()
                || evento.fechaEvento() == null) {
            throw new AmqpRejectAndDontRequeueException("Evento incompleto: " + evento);
        }
        if (!CategoriaCreadaEvento.TIPO.equals(evento.tipoEvento())) {
            throw new AmqpRejectAndDontRequeueException("Tipo de evento no soportado: " + evento.tipoEvento());
        }
        try {
            Instant.parse(evento.fechaEvento());
        } catch (DateTimeParseException error) {
            throw new AmqpRejectAndDontRequeueException("fechaEvento no es ISO-8601: " + evento.fechaEvento(), error);
        }
    }
}

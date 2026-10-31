package cl.duoc.mesatech.catalogo.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Convierte los eventos a JSON (Jackson 3, Spring AMQP 4).
 *
 * No se declaran aquí exchange, cola ni binding: la topología la crea el administrador
 * con infra/rabbitmq/configurar-recursos.sh y el usuario de la aplicación no tiene
 * permiso "configure" (principio de mínimo privilegio).
 */
@Configuration
public class RabbitConfig {

    @Bean
    MessageConverter conversorJson() {
        return new JacksonJsonMessageConverter();
    }
}

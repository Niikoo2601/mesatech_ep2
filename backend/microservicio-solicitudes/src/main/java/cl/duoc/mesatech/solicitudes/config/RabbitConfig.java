package cl.duoc.mesatech.solicitudes.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conversión JSON -> CategoriaCreadaEvento.
 * Se convierte siempre al tipo del parámetro del listener (tipo inferido), sin depender
 * del encabezado __TypeId__ que trae el nombre de la clase del publicador.
 *
 * La cola, el exchange y el binding NO se declaran aquí: los crea el administrador
 * con infra/rabbitmq/configurar-recursos.sh. El usuario consumidor solo tiene permiso de lectura.
 */
@Configuration
public class RabbitConfig {

    @Bean
    MessageConverter conversorJson() {
        JacksonJsonMessageConverter conversor = new JacksonJsonMessageConverter();
        conversor.setAlwaysConvertToInferredType(true);
        return conversor;
    }
}

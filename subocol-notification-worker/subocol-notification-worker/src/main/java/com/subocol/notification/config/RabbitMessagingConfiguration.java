package com.subocol.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.subocol.notification.event.EventType;

/**
 * Topologia del bus de eventos que consume el worker (contrato de dominio, seccion 3):
 * exchange {@code subocol.events} (topic, durable) con las routing keys
 * {@code AppointmentScheduled} y {@code AppointmentCancelled}.
 *
 * <p>El exchange lo publican los demas servicios con el Transactional Outbox; declararlo
 * aqui es idempotente porque son las mismas propiedades (topic y durable).
 */
@Configuration
@EnableConfigurationProperties(EventsProperties.class)
public class RabbitMessagingConfiguration {

	/**
	 * @return contenedor de escucha con un unico hilo: la idempotencia en memoria se
	 * resuelve consultando y marcando el mismo conjunto de eventos vistos, y con un solo
	 * hilo esa secuencia no tiene carreras.
	 */
	@Bean
	SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
			SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setConcurrentConsumers(1);
		factory.setMaxConcurrentConsumers(1);
		factory.setPrefetchCount(10);
		// El worker no tiene base de datos ni bandeja de reintentos: un evento que falla se
		// registra como error y se descarta, en vez de volver a la cola y bloquearla en
		// un ciclo de reintentos sin fin.
		factory.setDefaultRequeueRejected(false);
		return factory;
	}

	@Bean
	TopicExchange eventsExchange(EventsProperties properties) {
		return ExchangeBuilder.topicExchange(properties.exchange()).durable(true).build();
	}

	@Bean
	Queue notificationQueue(EventsProperties properties) {
		return QueueBuilder.durable(properties.queue()).build();
	}

	@Bean
	Binding appointmentScheduledBinding(Queue notificationQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(notificationQueue).to(eventsExchange).with(EventType.APPOINTMENT_SCHEDULED);
	}

	@Bean
	Binding appointmentCancelledBinding(Queue notificationQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(notificationQueue).to(eventsExchange).with(EventType.APPOINTMENT_CANCELLED);
	}

}

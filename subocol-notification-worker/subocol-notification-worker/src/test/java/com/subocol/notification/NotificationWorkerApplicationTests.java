package com.subocol.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.subocol.notification.config.EventsProperties;
import com.subocol.notification.messaging.AppointmentEventListener;
import com.subocol.notification.service.ProcessedEventRegistry;
import com.subocol.notification.service.WhatsAppNotifier;

/**
 * El worker arranca sin base de datos y sin servidor web. La prueba no levanta el broker:
 * el contenedor de escucha no arranca y el administrador dinamico de RabbitMQ no se crea,
 * para que la prueba no dependa de un broker disponible.
 */
@SpringBootTest(properties = { "spring.rabbitmq.listener.simple.auto-startup=false", "spring.rabbitmq.dynamic=false" })
class NotificationWorkerApplicationTests {

	@Autowired
	private AppointmentEventListener listener;

	@Autowired
	private WhatsAppNotifier notifier;

	@Autowired
	private ProcessedEventRegistry registry;

	@Autowired
	private EventsProperties eventsProperties;

	@Test
	void contextLoads() {
		assertThat(listener).isNotNull();
		assertThat(notifier).isNotNull();
		assertThat(registry).isNotNull();
		assertThat(eventsProperties.exchange()).isEqualTo("subocol.events");
		assertThat(eventsProperties.queue()).isEqualTo("subocol.notification-worker");
	}

}

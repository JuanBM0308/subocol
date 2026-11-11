package com.subocol.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventsPropertiesTest {

	@Test
	@DisplayName("Usa el exchange y la cola por defecto del taller cuando no vienen configurados")
	void fallsBackToTheDefaultExchangeAndQueue() {
		EventsProperties properties = new EventsProperties(null, "  ");

		assertThat(properties.exchange()).isEqualTo("subocol.events");
		assertThat(properties.queue()).isEqualTo("subocol.notification-worker");
	}

	@Test
	@DisplayName("Respeta el exchange y la cola configurados por variable de entorno")
	void keepsTheConfiguredExchangeAndQueue() {
		EventsProperties properties = new EventsProperties("otro.exchange", "otra.cola");

		assertThat(properties.exchange()).isEqualTo("otro.exchange");
		assertThat(properties.queue()).isEqualTo("otra.cola");
	}

}

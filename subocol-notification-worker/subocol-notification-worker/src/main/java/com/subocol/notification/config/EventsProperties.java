package com.subocol.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del bus de eventos del taller (contrato de dominio, seccion 3).
 *
 * @param exchange exchange topic y durable donde se publican los eventos del taller
 * @param queue cola propia del worker, con las dos routing keys de cita
 */
@ConfigurationProperties(prefix = "subocol.events")
public record EventsProperties(String exchange, String queue) {

	/** Exchange del taller: topic y durable. */
	public static final String DEFAULT_EXCHANGE = "subocol.events";

	/** Cola propia del worker. */
	public static final String DEFAULT_QUEUE = "subocol.notification-worker";

	public EventsProperties {
		exchange = (exchange == null || exchange.isBlank()) ? DEFAULT_EXCHANGE : exchange;
		queue = (queue == null || queue.isBlank()) ? DEFAULT_QUEUE : queue;
	}

}

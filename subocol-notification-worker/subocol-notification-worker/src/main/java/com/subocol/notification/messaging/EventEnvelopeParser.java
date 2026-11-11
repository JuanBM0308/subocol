package com.subocol.notification.messaging;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import com.subocol.notification.event.AppointmentPayload;
import com.subocol.notification.event.EventEnvelope;

/**
 * Lee el sobre de los eventos del bus y su payload (contrato de dominio, seccion 3).
 *
 * <p>El worker parsea el JSON a mano en vez de delegar en un converter de Spring AMQP:
 * el sobre es un contrato propio del taller y el unico formato que el worker acepta.
 */
@Component
public class EventEnvelopeParser {

	private final ObjectMapper objectMapper;

	public EventEnvelopeParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * @param body cuerpo del mensaje publicado en el bus
	 * @return el sobre del evento
	 * @throws tools.jackson.core.JacksonException si el cuerpo no es un sobre valido
	 */
	public EventEnvelope parse(byte[] body) {
		return objectMapper.readValue(body, EventEnvelope.class);
	}

	/**
	 * @param envelope sobre del evento
	 * @return el payload de la cita
	 * @throws tools.jackson.core.JacksonException si el payload no tiene la forma esperada
	 */
	public AppointmentPayload toPayload(EventEnvelope envelope) {
		return objectMapper.treeToValue(envelope.payload(), AppointmentPayload.class);
	}

}

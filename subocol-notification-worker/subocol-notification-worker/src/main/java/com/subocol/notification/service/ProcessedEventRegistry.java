package com.subocol.notification.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Idempotencia en memoria del worker (contrato de dominio, seccion 4).
 *
 * <p>El notification-worker es el unico componente del taller sin base de datos, asi que
 * no tiene la tabla {@code processed_events} que usa el appointment-service: guarda en un
 * conjunto en memoria los {@code eventId} ya atendidos y descarta los repetidos. El
 * conjunto vive en el heap del proceso, de modo que tolera reentregas del broker mientras
 * el worker no se reinicie; al reiniciar se pierde y un evento repetido volveria a
 * generar su mensaje.
 */
@Component
public class ProcessedEventRegistry {

	private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

	/**
	 * @param eventId identificador del evento
	 * @return {@code true} si el evento ya fue atendido antes
	 */
	public boolean isProcessed(String eventId) {
		return eventId != null && processedEventIds.contains(eventId);
	}

	/**
	 * @param eventId identificador del evento que ya se atendio
	 */
	public void markProcessed(String eventId) {
		if (eventId != null) {
			processedEventIds.add(eventId);
		}
	}

	/**
	 * @return cuantos eventos se tienen registrados
	 */
	public int size() {
		return processedEventIds.size();
	}

}

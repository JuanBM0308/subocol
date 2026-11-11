package com.subocol.notification.event;

import java.time.Instant;

import tools.jackson.databind.JsonNode;

/**
 * Sobre (envelope) de los eventos del bus, tal como lo fija el contrato de dominio
 * (seccion 3): {@code { eventId, eventType, occurredAt, aggregateId, payload }}.
 *
 * @param eventId identificador del evento; es la clave de la idempotencia en memoria
 * @param eventType nombre del evento, que es tambien su routing key
 * @param occurredAt momento en que ocurrio el evento, en UTC
 * @param aggregateId identificador de la entidad agregada (la orden)
 * @param payload contenido propio del evento
 */
public record EventEnvelope(String eventId, String eventType, Instant occurredAt, String aggregateId, JsonNode payload) {
}

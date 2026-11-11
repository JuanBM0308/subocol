package com.subocol.order.api.dto;

import java.util.UUID;

import com.subocol.order.domain.OrderStatus;

/**
 * Renglón del historial de estados tal como lo expone la API.
 *
 * @param eventId identificador del evento
 * @param fromStatus estado anterior, {@code null} en la primera fila
 * @param toStatus estado nuevo
 * @param reason motivo del cambio
 * @param changedBy identidad que ejecutó el cambio
 * @param changedAt fecha del cambio en ISO-8601 UTC con milisegundos
 * @param eventHash hash canónico del renglón
 * @param previousHash hash canónico del renglón anterior
 */
public record OrderHistoryEntry(UUID eventId, OrderStatus fromStatus, OrderStatus toStatus, String reason,
		String changedBy, String changedAt, String eventHash, String previousHash) {

}

package com.subocol.notification.event;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload de {@code AppointmentScheduled} y {@code AppointmentCancelled}
 * (contrato de dominio, seccion 3).
 *
 * @param appointmentId identificador de la cita
 * @param orderId identificador logico de la orden (el worker no consulta la base de ordenes)
 * @param advisorId asesor que atiende la cita
 * @param type tipo de la cita: {@code PICKUP} o {@code DELIVERY}
 * @param contactName nombre del cliente al que va dirigido el mensaje
 * @param contactPhone telefono del cliente al que va dirigido el mensaje
 * @param startsAt inicio de la cita, en UTC
 * @param cancelReason motivo de la cancelacion; {@code ORDER_CLOSED} en la automatica
 * @param notifyCustomer si el cliente debe recibir el mensaje. Es {@code false} en la
 * cancelacion automatica por cierre de orden y {@code true} en la cancelacion manual.
 * Se modela como {@link Boolean} y no como {@code boolean} para distinguir la ausencia
 * del campo de un {@code false} explicito.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentPayload(
		@JsonProperty("appointmentId") String appointmentId,
		@JsonProperty("orderId") String orderId,
		@JsonProperty("advisorId") String advisorId,
		@JsonProperty("type") String type,
		@JsonProperty("contactName") String contactName,
		@JsonProperty("contactPhone") String contactPhone,
		@JsonProperty("startsAt") Instant startsAt,
		@JsonProperty("cancelReason") String cancelReason,
		@JsonProperty("notifyCustomer") Boolean notifyCustomer) {
}

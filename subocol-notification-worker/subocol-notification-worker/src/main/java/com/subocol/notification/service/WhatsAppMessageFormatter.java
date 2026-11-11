package com.subocol.notification.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.subocol.notification.event.AppointmentPayload;

/**
 * Arma la linea del log de WhatsApp simulado con el formato exacto del contrato de
 * dominio (seccion 5):
 *
 * <pre>
 * WHATSAPP | to=3001234567 | type=APPOINTMENT_SCHEDULED | orderId=... | appointmentId=... | date=2026-11-12T09:00:00Z | Hola Ana Ruiz, tu cita para la entrega del vehiculo quedo agendada el 12 de noviembre a las 09:00.
 * </pre>
 *
 * <p>La fecha del mensaje y el campo {@code date} salen del mismo instante
 * ({@code startsAt}) en UTC: el cliente ve la hora de la cita, no la de su zona.
 */
@Component
public class WhatsAppMessageFormatter {

	/** Tipo del mensaje de cita agendada. */
	public static final String MESSAGE_TYPE_SCHEDULED = "APPOINTMENT_SCHEDULED";

	/** Tipo del mensaje de cita cancelada. */
	public static final String MESSAGE_TYPE_CANCELLED = "APPOINTMENT_CANCELLED";

	/** Prefijo de cada linea del log de WhatsApp simulado. */
	public static final String LOG_PREFIX = "WHATSAPP";

	private static final DateTimeFormatter ISO_UTC = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
			.withZone(ZoneOffset.UTC);

	private static final DateTimeFormatter TIME_OF_DAY = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

	private static final String[] MONTHS = { "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
			"septiembre", "octubre", "noviembre", "diciembre" };

	/**
	 * @param payload payload de {@code AppointmentScheduled}
	 * @return la linea del log de WhatsApp con la cita agendada
	 */
	public String formatScheduled(AppointmentPayload payload) {
		return format(payload, MESSAGE_TYPE_SCHEDULED,
				"Hola %s, tu cita para la entrega del vehiculo quedo agendada el %s a las %s.");
	}

	/**
	 * @param payload payload de {@code AppointmentCancelled}
	 * @return la linea del log de WhatsApp con la cita cancelada
	 */
	public String formatCancelled(AppointmentPayload payload) {
		return format(payload, MESSAGE_TYPE_CANCELLED, "Hola %s, tu cita del %s a las %s fue cancelada.");
	}

	private String format(AppointmentPayload payload, String messageType, String template) {
		LocalDateTime start = LocalDateTime.ofInstant(payload.startsAt(), ZoneOffset.UTC);
		String message = String.format(Locale.ROOT, template, payload.contactName(), date(start), TIME_OF_DAY.format(start));
		return LOG_PREFIX + " | to=" + payload.contactPhone()
				+ " | type=" + messageType
				+ " | orderId=" + payload.orderId()
				+ " | appointmentId=" + payload.appointmentId()
				+ " | date=" + ISO_UTC.format(payload.startsAt())
				+ " | " + message;
	}

	private static String date(LocalDateTime start) {
		return start.getDayOfMonth() + " de " + MONTHS[start.getMonthValue() - 1];
	}

}

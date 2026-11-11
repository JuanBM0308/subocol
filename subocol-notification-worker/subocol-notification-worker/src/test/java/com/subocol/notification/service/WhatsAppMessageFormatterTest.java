package com.subocol.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.subocol.notification.event.AppointmentPayload;

/**
 * El formato de la linea del log de WhatsApp simulado es un contrato con el cliente
 * (contrato de dominio, seccion 5): se verifica linea completa, no partes sueltas.
 */
class WhatsAppMessageFormatterTest {

	private static final String ORDER_ID = "3f9d2f0e-5f0e-4a2f-9d5f-2f0a1b2c3d4e";

	private static final String APPOINTMENT_ID = "b6f0a9c1-2d3e-4f50-8a6b-7c8d9e0f1a2b";

	private final WhatsAppMessageFormatter formatter = new WhatsAppMessageFormatter();

	@Test
	@DisplayName("Arma la linea de la cita agendada con el formato exacto del contrato")
	void buildsTheScheduledMessageLine() {
		AppointmentPayload payload = payload("2026-11-12T09:00:00Z");

		String line = formatter.formatScheduled(payload);

		assertThat(line).isEqualTo("WHATSAPP | to=3001234567 | type=APPOINTMENT_SCHEDULED | orderId=" + ORDER_ID
				+ " | appointmentId=" + APPOINTMENT_ID
				+ " | date=2026-11-12T09:00:00Z"
				+ " | Hola Ana Ruiz, tu cita para la entrega del vehiculo quedo agendada el 12 de noviembre a las 09:00.");
	}

	@Test
	@DisplayName("Arma la linea de la cita cancelada con el formato exacto del contrato")
	void buildsTheCancelledMessageLine() {
		AppointmentPayload payload = payload("2026-11-12T09:00:00Z");

		String line = formatter.formatCancelled(payload);

		assertThat(line).isEqualTo("WHATSAPP | to=3001234567 | type=APPOINTMENT_CANCELLED | orderId=" + ORDER_ID
				+ " | appointmentId=" + APPOINTMENT_ID
				+ " | date=2026-11-12T09:00:00Z"
				+ " | Hola Ana Ruiz, tu cita del 12 de noviembre a las 09:00 fue cancelada.");
	}

	@Test
	@DisplayName("Escribe la fecha y la hora de la cita en UTC, con el mes en espanol")
	void writesTheStartOfTheAppointmentInUtc() {
		AppointmentPayload payload = payload("2027-01-01T18:05:00Z");

		String line = formatter.formatScheduled(payload);

		assertThat(line).contains("date=2027-01-01T18:05:00Z");
		assertThat(line).endsWith("quedo agendada el 1 de enero a las 18:05.");
	}

	@Test
	@DisplayName("Normaliza a UTC la hora de la cita cuando el evento llega con offset")
	void normalizesTheStartOfTheAppointmentToUtc() {
		AppointmentPayload payload = payload("2026-11-12T04:00:00-05:00");

		String line = formatter.formatScheduled(payload);

		assertThat(line).contains("date=2026-11-12T09:00:00Z");
		assertThat(line).endsWith("quedo agendada el 12 de noviembre a las 09:00.");
	}

	private static AppointmentPayload payload(String startsAt) {
		return new AppointmentPayload(APPOINTMENT_ID, ORDER_ID, "asesor-1", "PICKUP", "Ana Ruiz", "3001234567",
				Instant.parse(startsAt), null, null);
	}

}

package com.subocol.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subocol.notification.event.AppointmentPayload;

@ExtendWith(MockitoExtension.class)
class WhatsAppNotifierTest {

	private static final String ORDER_ID = "3f9d2f0e-5f0e-4a2f-9d5f-2f0a1b2c3d4e";

	private static final String APPOINTMENT_ID = "b6f0a9c1-2d3e-4f50-8a6b-7c8d9e0f1a2b";

	@Mock
	private WhatsAppLogWriter writer;

	@Captor
	private ArgumentCaptor<String> lineCaptor;

	private WhatsAppNotifier notifier;

	@BeforeEach
	void setUp() {
		this.notifier = new WhatsAppNotifier(new WhatsAppMessageFormatter(), writer);
	}

	@Test
	@DisplayName("Escribe el mensaje de la cita agendada")
	void writesTheScheduledMessage() {
		boolean written = notifier.notifyScheduled(payload(null, null));

		assertThat(written).isTrue();
		verify(writer).write(lineCaptor.capture());
		assertThat(lineCaptor.getValue()).isEqualTo(
				"WHATSAPP | to=3001234567 | type=APPOINTMENT_SCHEDULED | orderId=" + ORDER_ID
						+ " | appointmentId=" + APPOINTMENT_ID
						+ " | date=2026-11-12T09:00:00Z"
						+ " | Hola Ana Ruiz, tu cita para la entrega del vehiculo quedo agendada el 12 de noviembre a las 09:00.");
	}

	@Test
	@DisplayName("Escribe el mensaje de la cita cancelada cuando el cliente debe ser notificado")
	void writesTheCancelledMessageWhenTheCustomerMustBeNotified() {
		boolean written = notifier.notifyCancelled(payload("ADVISOR_CANCELLED", Boolean.TRUE));

		assertThat(written).isTrue();
		verify(writer).write(lineCaptor.capture());
		assertThat(lineCaptor.getValue()).isEqualTo(
				"WHATSAPP | to=3001234567 | type=APPOINTMENT_CANCELLED | orderId=" + ORDER_ID
						+ " | appointmentId=" + APPOINTMENT_ID
						+ " | date=2026-11-12T09:00:00Z"
						+ " | Hola Ana Ruiz, tu cita del 12 de noviembre a las 09:00 fue cancelada.");
	}

	@Test
	@DisplayName("No escribe nada cuando la cancelacion es automatica por cierre de orden")
	void writesNothingOnTheAutomaticCancellation() {
		boolean written = notifier.notifyCancelled(payload("ORDER_CLOSED", Boolean.FALSE));

		assertThat(written).isFalse();
		verify(writer, never()).write(anyString());
	}

	@Test
	@DisplayName("No escribe nada cuando la cancelacion por cierre de orden llega sin el flag de notificacion")
	void writesNothingWhenTheOrderClosureReasonComesWithoutTheFlag() {
		boolean written = notifier.notifyCancelled(payload("ORDER_CLOSED", null));

		assertThat(written).isFalse();
		verify(writer, never()).write(anyString());
	}

	@Test
	@DisplayName("No escribe un mensaje incompleto")
	void writesNothingOnAnIncompletePayload() {
		AppointmentPayload incomplete = new AppointmentPayload(APPOINTMENT_ID, ORDER_ID, "asesor-1", "PICKUP", "Ana Ruiz",
				null, Instant.parse("2026-11-12T09:00:00Z"), null, null);

		boolean written = notifier.notifyScheduled(incomplete);

		assertThat(written).isFalse();
		verify(writer, never()).write(anyString());
	}

	@Test
	@DisplayName("No escribe un mensaje cuando el payload no llega")
	void writesNothingWithoutPayload() {
		boolean written = notifier.notifyScheduled(null);

		assertThat(written).isFalse();
		verify(writer, never()).write(anyString());
	}

	private static AppointmentPayload payload(String cancelReason, Boolean notifyCustomer) {
		return new AppointmentPayload(APPOINTMENT_ID, ORDER_ID, "asesor-1", "PICKUP", "Ana Ruiz", "3001234567",
				Instant.parse("2026-11-12T09:00:00Z"), cancelReason, notifyCustomer);
	}

}

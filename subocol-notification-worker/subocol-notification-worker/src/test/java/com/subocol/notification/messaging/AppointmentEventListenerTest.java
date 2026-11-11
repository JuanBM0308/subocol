package com.subocol.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import tools.jackson.databind.json.JsonMapper;

import com.subocol.notification.service.ProcessedEventRegistry;
import com.subocol.notification.service.StdoutWhatsAppLogWriter;
import com.subocol.notification.service.WhatsAppLogWriter;
import com.subocol.notification.service.WhatsAppMessageFormatter;
import com.subocol.notification.service.WhatsAppNotifier;

@ExtendWith(MockitoExtension.class)
class AppointmentEventListenerTest {

	private static final String ORDER_ID = "3f9d2f0e-5f0e-4a2f-9d5f-2f0a1b2c3d4e";

	private static final String APPOINTMENT_ID = "b6f0a9c1-2d3e-4f50-8a6b-7c8d9e0f1a2b";

	private static final String EVENT_ID = "9c1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8";

	@Mock
	private WhatsAppLogWriter writer;

	@Captor
	private ArgumentCaptor<String> lineCaptor;

	private final ProcessedEventRegistry registry = new ProcessedEventRegistry();

	private AppointmentEventListener listener;

	@BeforeEach
	void setUp() {
		this.listener = listenerWith(writer);
	}

	@Test
	@DisplayName("Escribe el mensaje de la cita agendada con el formato exacto")
	void writesTheScheduledMessage() {
		listener.onEvent(message(scheduledEvent()));

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
		listener.onEvent(message(cancelledEvent("ADVISOR_CANCELLED", true)));

		verify(writer).write(lineCaptor.capture());
		assertThat(lineCaptor.getValue()).isEqualTo(
				"WHATSAPP | to=3001234567 | type=APPOINTMENT_CANCELLED | orderId=" + ORDER_ID
						+ " | appointmentId=" + APPOINTMENT_ID
						+ " | date=2026-11-12T09:00:00Z"
						+ " | Hola Ana Ruiz, tu cita del 12 de noviembre a las 09:00 fue cancelada.");
	}

	@Test
	@DisplayName("No escribe nada cuando la cancelacion automatica trae notifyCustomer = false")
	void writesNothingOnTheAutomaticCancellation() {
		listener.onEvent(message(cancelledEvent("ORDER_CLOSED", false)));

		verify(writer, never()).write(anyString());
	}

	@Test
	@DisplayName("Ignora el evento repetido: la idempotencia en memoria evita el segundo mensaje")
	void ignoresTheRepeatedEvent() {
		Message repeated = message(scheduledEvent());

		listener.onEvent(repeated);
		listener.onEvent(repeated);

		verify(writer, times(1)).write(anyString());
		assertThat(registry.size()).isOne();
	}

	@Test
	@DisplayName("Ignora un tipo de evento que el worker no maneja")
	void ignoresAnUnsupportedEventType() {
		listener.onEvent(message(scheduledEvent().replace("AppointmentScheduled", "OrderClosed")));

		verify(writer, never()).write(anyString());
	}

	@Test
	@DisplayName("Descarta un evento ilegible sin escribir mensaje")
	void discardsAnUnreadableEvent() {
		Message broken = message("{ esto no es json ");

		assertThatCode(() -> listener.onEvent(broken)).doesNotThrowAnyException();
		verify(writer, never()).write(anyString());
	}

	@Test
	@DisplayName("Descarta un evento sin eventId, que no se puede usar para la idempotencia")
	void discardsAnEventWithoutId() {
		Message withoutId = message(scheduledEvent().replace(EVENT_ID, ""));

		assertThatCode(() -> listener.onEvent(withoutId)).doesNotThrowAnyException();
		verify(writer, never()).write(anyString());
	}

	@Test
	@DisplayName("Descarta un evento sin payload")
	void discardsAnEventWithoutPayload() {
		Message withoutPayload = message(scheduledEvent().replace("\"payload\": {", "\"payload\": null, \"unused\": {"));

		assertThatCode(() -> listener.onEvent(withoutPayload)).doesNotThrowAnyException();
		verify(writer, never()).write(anyString());
	}

	@Test
	@DisplayName("El mensaje sale por stdout tal cual, que es lo que revisa el cliente")
	void writesTheMessageToStandardOutput() {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
		try {
			listenerWith(new StdoutWhatsAppLogWriter()).onEvent(message(scheduledEvent()));
		}
		finally {
			System.setOut(originalOut);
		}

		// El log del worker tambien sale por stdout: la linea de WhatsApp es la primera.
		assertThat(captured.toString(StandardCharsets.UTF_8)).startsWith(
				"WHATSAPP | to=3001234567 | type=APPOINTMENT_SCHEDULED | orderId=" + ORDER_ID
						+ " | appointmentId=" + APPOINTMENT_ID
						+ " | date=2026-11-12T09:00:00Z"
						+ " | Hola Ana Ruiz, tu cita para la entrega del vehiculo quedo agendada el 12 de noviembre a las 09:00."
						+ System.lineSeparator());
	}

	private AppointmentEventListener listenerWith(WhatsAppLogWriter logWriter) {
		EventEnvelopeParser parser = new EventEnvelopeParser(JsonMapper.builder().build());
		WhatsAppNotifier notifier = new WhatsAppNotifier(new WhatsAppMessageFormatter(), logWriter);
		return new AppointmentEventListener(parser, notifier, registry);
	}

	private static Message message(String body) {
		return new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties());
	}

	private static String scheduledEvent() {
		return """
				{
				  "eventId": "%s",
				  "eventType": "AppointmentScheduled",
				  "occurredAt": "2026-11-11T15:04:05.123Z",
				  "aggregateId": "%s",
				  "payload": {
				    "appointmentId": "%s",
				    "orderId": "%s",
				    "advisorId": "asesor-1",
				    "type": "PICKUP",
				    "contactName": "Ana Ruiz",
				    "contactPhone": "3001234567",
				    "startsAt": "2026-11-12T09:00:00Z"
				  }
				}
				""".formatted(EVENT_ID, ORDER_ID, APPOINTMENT_ID, ORDER_ID);
	}

	private static String cancelledEvent(String cancelReason, boolean notifyCustomer) {
		return """
				{
				  "eventId": "%s",
				  "eventType": "AppointmentCancelled",
				  "occurredAt": "2026-11-12T10:15:00.000Z",
				  "aggregateId": "%s",
				  "payload": {
				    "appointmentId": "%s",
				    "orderId": "%s",
				    "advisorId": "asesor-1",
				    "type": "PICKUP",
				    "contactName": "Ana Ruiz",
				    "contactPhone": "3001234567",
				    "startsAt": "2026-11-12T09:00:00Z",
				    "cancelReason": "%s",
				    "notifyCustomer": %s
				  }
				}
				""".formatted(EVENT_ID, ORDER_ID, APPOINTMENT_ID, ORDER_ID, cancelReason, notifyCustomer);
	}

}

package com.subocol.notification.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.subocol.notification.event.AppointmentPayload;
import com.subocol.notification.event.EventEnvelope;
import com.subocol.notification.event.EventType;
import com.subocol.notification.service.ProcessedEventRegistry;
import com.subocol.notification.service.WhatsAppNotifier;

/**
 * Consume los eventos de cita del bus y produce el log de WhatsApp simulado
 * (contrato de dominio, secciones 3, 4 y 5).
 *
 * <p>El consumidor es idempotente en memoria: un {@code eventId} ya atendido se descarta
 * sin escribir otra linea.
 */
@Component
public class AppointmentEventListener {

	private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);

	private final EventEnvelopeParser parser;

	private final WhatsAppNotifier notifier;

	private final ProcessedEventRegistry registry;

	public AppointmentEventListener(EventEnvelopeParser parser, WhatsAppNotifier notifier,
			ProcessedEventRegistry registry) {
		this.parser = parser;
		this.notifier = notifier;
		this.registry = registry;
	}

	/**
	 * @param message mensaje recibido del exchange {@code subocol.events}
	 */
	@RabbitListener(queues = "${subocol.events.queue}", containerFactory = "rabbitListenerContainerFactory")
	public void onEvent(Message message) {
		EventEnvelope envelope;
		try {
			envelope = parser.parse(message.getBody());
		}
		catch (RuntimeException e) {
			log.error("Evento ilegible: se descarta sin escribir mensaje", e);
			return;
		}
		if (envelope.eventId() == null || envelope.eventId().isBlank()) {
			log.error("Evento sin eventId: se descarta sin escribir mensaje");
			return;
		}
		if (registry.isProcessed(envelope.eventId())) {
			log.info("Evento ya procesado: se ignora. eventId={}", envelope.eventId());
			return;
		}
		handle(envelope);
		registry.markProcessed(envelope.eventId());
	}

	private void handle(EventEnvelope envelope) {
		if (EventType.APPOINTMENT_SCHEDULED.equals(envelope.eventType())) {
			AppointmentPayload payload = payload(envelope);
			if (payload != null && notifier.notifyScheduled(payload)) {
				log.info("Mensaje de cita agendada escrito. eventId={}, appointmentId={}", envelope.eventId(),
						payload.appointmentId());
			}
			return;
		}
		if (EventType.APPOINTMENT_CANCELLED.equals(envelope.eventType())) {
			AppointmentPayload payload = payload(envelope);
			if (payload != null && notifier.notifyCancelled(payload)) {
				log.info("Mensaje de cita cancelada escrito. eventId={}, appointmentId={}", envelope.eventId(),
						payload.appointmentId());
			}
			return;
		}
		log.warn("Tipo de evento no soportado: se ignora. eventType={}, eventId={}", envelope.eventType(),
				envelope.eventId());
	}

	private AppointmentPayload payload(EventEnvelope envelope) {
		if (envelope.payload() == null) {
			log.error("Evento sin payload: se descarta sin escribir mensaje. eventId={}", envelope.eventId());
			return null;
		}
		try {
			return parser.toPayload(envelope);
		}
		catch (RuntimeException e) {
			log.error("Payload ilegible: se descarta sin escribir mensaje. eventId={}", envelope.eventId(), e);
			return null;
		}
	}

}

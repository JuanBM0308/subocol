package com.subocol.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.subocol.notification.event.AppointmentPayload;

/**
 * Decide si un evento de cita genera mensaje para el cliente y lo escribe.
 *
 * <p>Regla dura del taller (contrato de dominio, seccion 3): la cancelacion automatica por
 * cierre de orden llega con {@code notifyCustomer = false} y <strong>no</strong> genera
 * ninguna linea del log de WhatsApp, porque el cliente ya recogio su vehiculo. La
 * cancelacion manual llega con {@code notifyCustomer = true} y si genera mensaje.
 */
@Component
public class WhatsAppNotifier {

	private static final Logger log = LoggerFactory.getLogger(WhatsAppNotifier.class);

	/** Motivo de la cancelacion automatica por cierre de orden. */
	public static final String CANCEL_REASON_ORDER_CLOSED = "ORDER_CLOSED";

	private final WhatsAppMessageFormatter formatter;

	private final WhatsAppLogWriter writer;

	public WhatsAppNotifier(WhatsAppMessageFormatter formatter, WhatsAppLogWriter writer) {
		this.formatter = formatter;
		this.writer = writer;
	}

	/**
	 * @param payload payload de {@code AppointmentScheduled}
	 * @return {@code true} si se escribio la linea del log de WhatsApp
	 */
	public boolean notifyScheduled(AppointmentPayload payload) {
		if (!isComplete(payload)) {
			log.error("Evento de cita agendada incompleto: no se escribe mensaje. appointmentId={}",
					payload == null ? null : payload.appointmentId());
			return false;
		}
		writer.write(formatter.formatScheduled(payload));
		return true;
	}

	/**
	 * @param payload payload de {@code AppointmentCancelled}
	 * @return {@code true} si se escribio la linea del log de WhatsApp; {@code false} si la
	 * cancelacion es automatica y por lo tanto no se notifica al cliente
	 */
	public boolean notifyCancelled(AppointmentPayload payload) {
		if (!isComplete(payload)) {
			log.error("Evento de cita cancelada incompleto: no se escribe mensaje. appointmentId={}",
					payload == null ? null : payload.appointmentId());
			return false;
		}
		if (suppressesNotification(payload)) {
			log.info("Cancelacion automatica por cierre de orden: no se notifica al cliente. appointmentId={}, cancelReason={}",
					payload.appointmentId(), payload.cancelReason());
			return false;
		}
		writer.write(formatter.formatCancelled(payload));
		return true;
	}

	/**
	 * @param payload payload de {@code AppointmentCancelled}
	 * @return {@code true} si la cancelacion no debe generar mensaje para el cliente
	 */
	public static boolean suppressesNotification(AppointmentPayload payload) {
		return Boolean.FALSE.equals(payload.notifyCustomer())
				|| CANCEL_REASON_ORDER_CLOSED.equals(payload.cancelReason());
	}

	private static boolean isComplete(AppointmentPayload payload) {
		return payload != null
				&& isPresent(payload.appointmentId())
				&& isPresent(payload.orderId())
				&& isPresent(payload.contactName())
				&& isPresent(payload.contactPhone())
				&& payload.startsAt() != null;
	}

	private static boolean isPresent(String value) {
		return value != null && !value.isBlank();
	}

}

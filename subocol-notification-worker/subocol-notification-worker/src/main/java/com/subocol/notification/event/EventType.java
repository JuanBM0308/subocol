package com.subocol.notification.event;

/**
 * Eventos del bus que consume el worker (contrato de dominio, seccion 3).
 *
 * <p>El nombre del evento es tambien su routing key en el exchange {@code subocol.events}.
 */
public final class EventType {

	/** Cita agendada: se avisa al cliente que su cita quedo agendada. */
	public static final String APPOINTMENT_SCHEDULED = "AppointmentScheduled";

	/** Cita cancelada: se avisa al cliente solo si el evento pide notificarlo. */
	public static final String APPOINTMENT_CANCELLED = "AppointmentCancelled";

	private EventType() {
	}

}

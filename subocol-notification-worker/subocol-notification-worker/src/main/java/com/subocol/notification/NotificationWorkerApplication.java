package com.subocol.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Worker de notificaciones del Taller 360 Subocol.
 *
 * <p>Es el unico componente del taller sin base de datos: consume los eventos de cita
 * del exchange {@code subocol.events} y escribe el log de WhatsApp simulado en
 * {@code stdout}, que es lo que el cliente revisa (contrato de dominio, secciones 3, 4
 * y 5).
 */
@SpringBootApplication
public class NotificationWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationWorkerApplication.class, args);
	}

}

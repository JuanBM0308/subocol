package com.subocol.notification.service;

import org.springframework.stereotype.Component;

/**
 * Escribe el log de WhatsApp simulado en {@code stdout} del contenedor.
 *
 * <p>La linea sale directa de {@code stdout} y no por el appender de logs: el formato que
 * revisa el cliente no admite el envoltorio de log estructurado, que es la excepcion
 * deliberada que el taller acepta para este worker.
 */
@Component
public class StdoutWhatsAppLogWriter implements WhatsAppLogWriter {

	@Override
	public void write(String line) {
		System.out.println(line);
	}

}

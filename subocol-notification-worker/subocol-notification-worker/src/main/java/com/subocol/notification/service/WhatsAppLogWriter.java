package com.subocol.notification.service;

/**
 * Destino del log de WhatsApp simulado (contrato de dominio, seccion 5).
 *
 * <p>La implementacion de produccion escribe en {@code stdout}; las pruebas usan un doble
 * para verificar el formato exacto de la linea.
 */
public interface WhatsAppLogWriter {

	/**
	 * @param line linea completa del log de WhatsApp simulado
	 */
	void write(String line);

}

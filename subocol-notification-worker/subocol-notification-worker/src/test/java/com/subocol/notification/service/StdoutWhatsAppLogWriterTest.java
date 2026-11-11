package com.subocol.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El log de WhatsApp simulado es lo que el cliente revisa: tiene que salir por
 * {@code stdout} tal cual, sin envoltorio de log estructurado.
 */
class StdoutWhatsAppLogWriterTest {

	private final PrintStream originalOut = System.out;

	private ByteArrayOutputStream captured;

	@BeforeEach
	void setUp() {
		this.captured = new ByteArrayOutputStream();
		System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
	}

	@Test
	@DisplayName("Escribe la linea en stdout")
	void writesTheLineToStandardOutput() {
		new StdoutWhatsAppLogWriter().write("WHATSAPP | to=3001234567 | type=APPOINTMENT_SCHEDULED");

		assertThat(captured.toString(StandardCharsets.UTF_8))
				.isEqualTo("WHATSAPP | to=3001234567 | type=APPOINTMENT_SCHEDULED" + System.lineSeparator());
	}

}

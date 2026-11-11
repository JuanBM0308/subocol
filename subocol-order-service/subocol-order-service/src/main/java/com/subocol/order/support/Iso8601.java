package com.subocol.order.support;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formato de fecha y hora del contrato de integración: ISO-8601 en UTC con
 * milisegundos ({@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}).
 */
public final class Iso8601 {

	public static final DateTimeFormatter UTC_MILLIS = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
			.withZone(ZoneOffset.UTC);

	private Iso8601() {
	}

	public static String format(Instant instant) {
		return instant == null ? null : UTC_MILLIS.format(instant);
	}

}

package com.subocol.order.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.subocol.order.support.Iso8601;

/**
 * Hash canónico del historial de estados.
 *
 * <p>El hash de un renglón es el SHA-256 en hexadecimal minúsculas de la
 * concatenación, en UTF-8, de los ocho campos del renglón en este orden:
 * {@code eventId}, {@code orderId}, {@code fromStatus}, {@code toStatus},
 * {@code reason}, {@code changedBy}, {@code changedAt} y {@code previousHash}.
 *
 * <p>Cada campo se escribe como {@code <longitudEnBytes>:<valor>} y los
 * fragmentos se unen con {@code |}. Un valor nulo se escribe {@code ~} y uno
 * vacío {@code 0:}. Los UUID van en forma textual RFC 4122 en minúsculas y
 * {@code changedAt} en ISO-8601 UTC con milisegundos.
 */
@Service
public class HistoryHashService {

	/**
	 * {@code previousHash} de la primera fila del historial de cada orden: 64
	 * ceros.
	 */
	public static final String GENESIS_HASH = "0".repeat(64);

	private static final String NULL_MARKER = "~";

	private static final String FIELD_SEPARATOR = "|";

	public String compute(UUID eventId, UUID orderId, String fromStatus, String toStatus, String reason,
			String changedBy, Instant changedAt, String previousHash) {
		return sha256Hex(canonicalPayload(eventId, orderId, fromStatus, toStatus, reason, changedBy, changedAt,
				previousHash));
	}

	public String canonicalPayload(UUID eventId, UUID orderId, String fromStatus, String toStatus, String reason,
			String changedBy, Instant changedAt, String previousHash) {
		return String.join(FIELD_SEPARATOR, field(textOf(eventId)), field(textOf(orderId)), field(fromStatus),
				field(toStatus), field(reason), field(changedBy), field(Iso8601.format(changedAt)),
				field(previousHash));
	}

	private static String textOf(UUID uuid) {
		return uuid == null ? null : uuid.toString();
	}

	private static String field(String value) {
		if (value == null) {
			return NULL_MARKER;
		}
		return value.getBytes(StandardCharsets.UTF_8).length + ":" + value;
	}

	private static String sha256Hex(String payload) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available in this JVM", ex);
		}
	}

}

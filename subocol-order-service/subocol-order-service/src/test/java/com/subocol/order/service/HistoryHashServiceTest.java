package com.subocol.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HistoryHashServiceTest {

	private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private static final Instant CHANGED_AT = Instant.parse("2026-11-11T15:04:05.123Z");

	private final HistoryHashService historyHashService = new HistoryHashService();

	@Test
	@DisplayName("El payload canónico es el del contrato y el hash es su SHA-256")
	void computesTheCanonicalHash() {
		String expectedPayload = "36:11111111-1111-1111-1111-111111111111"
				+ "|36:22222222-2222-2222-2222-222222222222"
				+ "|~"
				+ "|8:RECIBIDA"
				+ "|16:Orden registrada"
				+ "|11:dev-advisor"
				+ "|24:2026-11-11T15:04:05.123Z"
				+ "|64:" + HistoryHashService.GENESIS_HASH;

		String payload = historyHashService.canonicalPayload(EVENT_ID, ORDER_ID, null, "RECIBIDA",
				"Orden registrada", "dev-advisor", CHANGED_AT, HistoryHashService.GENESIS_HASH);

		assertThat(payload).isEqualTo(expectedPayload);
		assertThat(historyHashService.compute(EVENT_ID, ORDER_ID, null, "RECIBIDA", "Orden registrada",
				"dev-advisor", CHANGED_AT, HistoryHashService.GENESIS_HASH)).isEqualTo(sha256Hex(expectedPayload));
	}

	@Test
	@DisplayName("La génesis del historial son 64 ceros")
	void usesSixtyFourZerosAsTheGenesisHash() {
		assertThat(HistoryHashService.GENESIS_HASH).hasSize(64).matches("0{64}");

		String genesisHash = historyHashService.compute(EVENT_ID, ORDER_ID, null, "RECIBIDA", "Orden registrada",
				"dev-advisor", CHANGED_AT, HistoryHashService.GENESIS_HASH);
		String otherHash = historyHashService.compute(EVENT_ID, ORDER_ID, null, "RECIBIDA", "Orden registrada",
				"dev-advisor", CHANGED_AT, "1".repeat(64));

		assertThat(genesisHash).hasSize(64).matches("[0-9a-f]{64}");
		assertThat(genesisHash).isNotEqualTo(otherHash);
	}

	@Test
	@DisplayName("El hash es estable para la misma entrada")
	void isStableForTheSameInput() {
		String first = historyHashService.compute(EVENT_ID, ORDER_ID, null, "RECIBIDA", "Orden registrada",
				"dev-advisor", CHANGED_AT, HistoryHashService.GENESIS_HASH);
		String second = historyHashService.compute(EVENT_ID, ORDER_ID, null, "RECIBIDA", "Orden registrada",
				"dev-advisor", CHANGED_AT, HistoryHashService.GENESIS_HASH);

		assertThat(first).isEqualTo(second);
	}

	@Test
	@DisplayName("Un valor nulo se escribe como ~ y uno vacío como 0:")
	void encodesNullAndEmptyValues() {
		String payload = historyHashService.canonicalPayload(EVENT_ID, ORDER_ID, null, "RECIBIDA", "",
				"dev-advisor", CHANGED_AT, HistoryHashService.GENESIS_HASH);

		assertThat(payload).contains("|~|").contains("|0:|");
	}

	@Test
	@DisplayName("El prefijo cuenta bytes UTF-8 y no caracteres")
	void countsUtf8BytesAndNotCharacters() {
		String payload = historyHashService.canonicalPayload(EVENT_ID, ORDER_ID, null, "RECIBIDA", "Añadir",
				"dev-advisor", CHANGED_AT, HistoryHashService.GENESIS_HASH);

		assertThat(payload).contains("|7:Añadir|");
	}

	@Test
	@DisplayName("changedAt se escribe en ISO-8601 UTC con milisegundos")
	void formatsChangedAtInUtcWithMilliseconds() {
		String payload = historyHashService.canonicalPayload(EVENT_ID, ORDER_ID, null, "RECIBIDA", "Orden registrada",
				"dev-advisor", CHANGED_AT, HistoryHashService.GENESIS_HASH);

		assertThat(payload).contains("|24:2026-11-11T15:04:05.123Z|");
	}

	private static String sha256Hex(String payload) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}

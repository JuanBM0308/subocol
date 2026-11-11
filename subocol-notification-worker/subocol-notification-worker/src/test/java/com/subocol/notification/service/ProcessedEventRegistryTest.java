package com.subocol.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProcessedEventRegistryTest {

	private final ProcessedEventRegistry registry = new ProcessedEventRegistry();

	@Test
	@DisplayName("Un evento visto por primera vez no esta procesado")
	void anEventIsNotProcessedTheFirstTime() {
		assertThat(registry.isProcessed("9c1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8")).isFalse();
	}

	@Test
	@DisplayName("Un evento marcado queda procesado")
	void aMarkedEventIsProcessed() {
		registry.markProcessed("9c1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8");

		assertThat(registry.isProcessed("9c1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8")).isTrue();
		assertThat(registry.size()).isOne();
	}

	@Test
	@DisplayName("Un evento sin identificador no se registra")
	void anEventWithoutIdIsNotRegistered() {
		registry.markProcessed(null);

		assertThat(registry.isProcessed(null)).isFalse();
		assertThat(registry.size()).isZero();
	}

}

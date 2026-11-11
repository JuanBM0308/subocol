package com.subocol.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Pruebas de la clasificacion de fallos del servicio de destino.
 */
class UpstreamFailureClassifierTest {

	private final UpstreamFailureClassifier classifier = new UpstreamFailureClassifier();

	@Test
	@DisplayName("un tiempo de espera agotado se responde con 504")
	void classifiesTimeoutAsGatewayTimeout() {
		ProblemDetail problem = this.classifier.classify(new HttpTimeoutException("timeout"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
		assertThat(problem.getTitle()).isEqualTo("Tiempo de espera agotado");
	}

	@Test
	@DisplayName("un tiempo de espera de socket se responde con 504")
	void classifiesSocketTimeoutAsGatewayTimeout() {
		assertThat(this.classifier.classify(new SocketTimeoutException("timeout")).getStatus())
				.isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
		assertThat(this.classifier.classify(new TimeoutException("timeout")).getStatus())
				.isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
	}

	@Test
	@DisplayName("una conexion rechazada se responde con 502")
	void classifiesConnectionFailureAsBadGateway() {
		ProblemDetail problem = this.classifier.classify(new ConnectException("connection refused"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
		assertThat(problem.getTitle()).isEqualTo("Servicio de destino no disponible");
		assertThat(problem.getDetail()).contains("establecer conexión");
	}

	@Test
	@DisplayName("un host desconocido se responde con 502")
	void classifiesUnknownHostAsBadGateway() {
		assertThat(this.classifier.classify(new UnknownHostException("order-service")).getStatus())
				.isEqualTo(HttpStatus.BAD_GATEWAY.value());
	}

	@Test
	@DisplayName("un fallo inesperado se responde con 502 sin filtrar la traza interna")
	void classifiesUnexpectedFailureAsBadGateway() {
		ProblemDetail problem = this.classifier.classify(new IllegalStateException("boom"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
		assertThat(problem.getDetail()).contains("respuesta válida");
		assertThat(problem.getDetail()).doesNotContain("boom");
	}

	@Test
	@DisplayName("el fallo se busca tambien en las causas encadenadas")
	void looksIntoTheCauseChain() {
		IOException cause = new HttpTimeoutException("timeout");
		RuntimeException wrapped = new RuntimeException("wrapper", cause);

		assertThat(this.classifier.isTimeout(wrapped)).isTrue();
		assertThat(this.classifier.isConnectionFailure(wrapped)).isFalse();
	}

	@Test
	@DisplayName("una lectura bloqueada agotada se reconoce como tiempo de espera")
	void recognisesBlockedReadTimeout() {
		IllegalStateException blocked = new IllegalStateException("Timeout on blocking read for 1000000000 NANOSECONDS");

		assertThat(this.classifier.isTimeout(blocked)).isTrue();
	}

	@Test
	@DisplayName("un fallo sin causa no es un tiempo de espera")
	void plainFailureIsNotTimeout() {
		assertThat(this.classifier.isTimeout(new IllegalStateException("boom"))).isFalse();
		assertThat(this.classifier.isConnectionFailure(new IllegalStateException("boom"))).isFalse();
	}

	@Test
	@DisplayName("un fallo que se apunta a si mismo no bouclea")
	void selfReferencingCauseDoesNotLoop() {
		RuntimeException loop = new RuntimeException("loop") {
			@Override
			public synchronized Throwable getCause() {
				return this;
			}
		};

		assertThat(this.classifier.isTimeout(loop)).isFalse();
		assertThat(this.classifier.isConnectionFailure(loop)).isFalse();
	}

}

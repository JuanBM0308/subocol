package com.subocol.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Pruebas de los mensajes que viajan entre el gateway y el servicio de destino.
 */
class UpstreamMessageTest {

	@Test
	@DisplayName("la peticion conserva metodo, direccion, cabeceras y cuerpo")
	void requestKeepsEverythingItCarries() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Correlation-Id", "abc-123");
		byte[] body = "{\"customerName\":\"Ana Ruiz\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

		UpstreamRequest request = new UpstreamRequest(HttpMethod.POST,
				java.net.URI.create("http://order-service:8082/api/orders"), headers, body);

		assertThat(request.method()).isEqualTo(HttpMethod.POST);
		assertThat(request.uri()).hasToString("http://order-service:8082/api/orders");
		assertThat(request.headers().getFirst("X-Correlation-Id")).isEqualTo("abc-123");
		assertThat(new String(request.body(), java.nio.charset.StandardCharsets.UTF_8))
				.isEqualTo("{\"customerName\":\"Ana Ruiz\"}");
	}

	@Test
	@DisplayName("la respuesta conserva estado, cabeceras y cuerpo")
	void responseKeepsEverythingItCarries() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		byte[] body = "{\"orderId\":\"3f9d\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

		UpstreamResponse response = new UpstreamResponse(201, headers, body);

		assertThat(response.status()).isEqualTo(201);
		assertThat(response.headers().getFirst("Content-Type")).isEqualTo("application/json");
		assertThat(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8))
				.isEqualTo("{\"orderId\":\"3f9d\"}");
	}

}

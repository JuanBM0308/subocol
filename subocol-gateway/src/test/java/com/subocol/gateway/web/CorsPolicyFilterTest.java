package com.subocol.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.subocol.gateway.config.GatewayProperties;

/**
 * Pruebas de la politica de origenes cruzados que aplica el gateway.
 */
class CorsPolicyFilterTest {

	private static final String ALLOWED_ORIGIN = "http://localhost:8080";

	private static CorsPolicyFilter filterWith(String... allowedOrigins) {
		GatewayProperties properties = new GatewayProperties();
		properties.getCors().setAllowedOrigins(List.of(allowedOrigins));
		return new CorsPolicyFilter(properties);
	}

	@Test
	@DisplayName("acepta el origen declarado en la configuracion")
	void acceptsConfiguredOrigin() throws Exception {
		CorsPolicyFilter filter = filterWith(ALLOWED_ORIGIN);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
		request.addHeader("Origin", ALLOWED_ORIGIN);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, mock(jakarta.servlet.FilterChain.class));

		assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
		assertThat(response.getHeader("Access-Control-Allow-Methods")).contains("POST");
		assertThat(response.getHeader("Vary")).isEqualTo("Origin");
	}

	@Test
	@DisplayName("rechaza la peticion cuando el origen no esta declarado")
	void rejectsUndeclaredOrigin() throws Exception {
		CorsPolicyFilter filter = filterWith(ALLOWED_ORIGIN);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
		request.addHeader("Origin", "http://atacante.example");
		MockHttpServletResponse response = new MockHttpServletResponse();
		jakarta.servlet.FilterChain chain = mock(jakarta.servlet.FilterChain.class);

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
		verify(chain, never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("responde al preflight sin reenviar la peticion")
	void answersPreflightWithoutForwarding() throws Exception {
		CorsPolicyFilter filter = filterWith(ALLOWED_ORIGIN);
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/orders");
		request.addHeader("Origin", ALLOWED_ORIGIN);
		MockHttpServletResponse response = new MockHttpServletResponse();
		jakarta.servlet.FilterChain chain = mock(jakarta.servlet.FilterChain.class);

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(204);
		assertThat(response.getHeader("Access-Control-Max-Age")).isEqualTo("3600");
		verify(chain, never()).doFilter(any(), any());
	}

}

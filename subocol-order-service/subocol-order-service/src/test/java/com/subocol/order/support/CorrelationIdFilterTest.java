package com.subocol.order.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter correlationIdFilter = new CorrelationIdFilter();

	@Test
	@DisplayName("Reutiliza el identificador de correlación que llega en la cabecera")
	void reusesTheCorrelationIdFromTheRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
		request.addHeader(CorrelationIdFilter.HEADER_NAME, "corr-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		correlationIdFilter.doFilter(request, response, noOpChain());

		assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("corr-123");
	}

	@Test
	@DisplayName("Genera un identificador de correlación cuando la petición no trae uno")
	void generatesACorrelationIdWhenTheRequestHasNone() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
		MockHttpServletResponse response = new MockHttpServletResponse();

		correlationIdFilter.doFilter(request, response, noOpChain());

		String correlationId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
		assertThat(correlationId).isNotBlank();
		assertThatCode(() -> UUID.fromString(correlationId)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Deja el identificador de correlación en el MDC durante la petición")
	void putsTheCorrelationIdInTheMdc() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
		request.addHeader(CorrelationIdFilter.HEADER_NAME, "corr-123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> correlationIdSeenByTheChain = new AtomicReference<>();

		FilterChain chain = (servletRequest, servletResponse) -> correlationIdSeenByTheChain
				.set(MDC.get(CorrelationIdFilter.MDC_KEY));

		correlationIdFilter.doFilter(request, response, chain);

		assertThat(correlationIdSeenByTheChain.get()).isEqualTo("corr-123");
		assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
	}

	private static FilterChain noOpChain() {
		return (servletRequest, servletResponse) -> {
		};
	}

}

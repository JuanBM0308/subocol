package com.subocol.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

/**
 * Pruebas de la generacion y propagacion del identificador de correlacion.
 */
class CorrelationIdPropagationFilterTest {

	private static final String HEADER = CorrelationIdPropagationFilter.HEADER;

	@Test
	@DisplayName("genera un identificador cuando la peticion no lo trae")
	void generatesIdentifierWhenMissing() throws Exception {
		CorrelationIdPropagationFilter filter = new CorrelationIdPropagationFilter();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, mock(FilterChain.class));

		String correlationId = response.getHeader(HEADER);
		assertThat(correlationId).isNotNull().isNotBlank();
		// El identificador generado debe ser un UUID.
		assertThat(UUID.fromString(correlationId)).hasToString(correlationId);
	}

	@Test
	@DisplayName("propaga el identificador que trae la peticion")
	void propagatesIncomingIdentifier() throws Exception {
		CorrelationIdPropagationFilter filter = new CorrelationIdPropagationFilter();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
		request.addHeader(HEADER, "abc-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, mock(FilterChain.class));

		assertThat(response.getHeader(HEADER)).isEqualTo("abc-123");
	}

	@Test
	@DisplayName("deja el identificador en el MDC durante el processamento")
	void publishesIdentifierInMdc() throws Exception {
		CorrelationIdPropagationFilter filter = new CorrelationIdPropagationFilter();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
		request.addHeader(HEADER, "abc-123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> observed = new AtomicReference<>();
		FilterChain chain = mock(FilterChain.class);
		// El identificador se observa mientras la cadena procesa la peticion.
		doAnswer(invocation -> {
			observed.set(MDC.get(CorrelationIdPropagationFilter.MDC_KEY));
			return null;
		}).when(chain).doFilter(any(), any());

		filter.doFilter(request, response, chain);

		assertThat(observed.get()).isEqualTo("abc-123");
		// Al terminar la peticion el MDC no debe conservar el identificador.
		assertThat(MDC.get(CorrelationIdPropagationFilter.MDC_KEY)).isNull();
		verify(chain).doFilter(any(), any());
	}

}

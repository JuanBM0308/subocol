package com.subocol.gateway.web;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Genera o propaga el identificador de correlacion de la peticion.
 *
 * <p>La cabecera {@code X-Correlation-Id} viaja del navegador al gateway y del gateway
 * al servicio de destino. Ademas queda en el MDC, de modo que cada linea de log del
 * gateway lleva el identificador de la peticion.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdPropagationFilter extends OncePerRequestFilter {

	/** Cabecera de entrada y de salida. */
	public static final String HEADER = "X-Correlation-Id";

	/** Clave del MDC. */
	public static final String MDC_KEY = "correlationId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String correlationId = request.getHeader(HEADER);
		if (correlationId == null || correlationId.isBlank()) {
			correlationId = UUID.randomUUID().toString();
		}
		MDC.put(MDC_KEY, correlationId);
		response.setHeader(HEADER, correlationId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove(MDC_KEY);
		}
	}

}

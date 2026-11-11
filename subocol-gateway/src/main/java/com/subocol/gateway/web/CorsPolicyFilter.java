package com.subocol.gateway.web;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.subocol.gateway.config.GatewayProperties;

/**
 * Politica de origenes cruzados del gateway.
 *
 * <p>El flujo normal del taller no la necesita (nginx hace de proxy inverso y el
 * navegador habla siempre con el mismo origen), asi que queda como control en
 * profundidad: solo se acepta el origen declarado en la configuracion, sin
 * comodines y sin credenciales cruzadas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CorsPolicyFilter extends OncePerRequestFilter {

	private final List<String> allowedOrigins;

	public CorsPolicyFilter(GatewayProperties properties) {
		this.allowedOrigins = List.copyOf(properties.getCors().getAllowedOrigins());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String origin = request.getHeader("Origin");
		boolean allowed = origin != null && allowedOrigins.contains(origin);
		if (allowed) {
			response.setHeader("Access-Control-Allow-Origin", origin);
			response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
			response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Correlation-Id, Accept");
			response.setHeader("Access-Control-Max-Age", "3600");
		}
		response.addHeader("Vary", "Origin");
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}
		chain.doFilter(request, response);
	}

}

package com.subocol.gateway.web;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import com.subocol.gateway.config.GatewayProperties;
import com.subocol.gateway.routing.GatewayRoute;
import com.subocol.gateway.routing.RouteTable;

/**
 * Reenvia las peticiones de la API al servicio de destino.
 *
 * <p>El gateway no reescribe el recurso: solo cambia el host. El camino, la cadena de
 * consulta, el metodo, las cabeceras y el cuerpo viajan tal cual, y la respuesta del
 * servicio de destino se devuelve sin transformar.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class ApiProxyFilter extends OncePerRequestFilter {

	/** Cabeceras que pertenecen a la conexion y no se reenvian. */
	private static final Set<String> HOP_BY_HOP = Set.of("connection", "content-length", "host",
			"keep-alive", "proxy-authenticate", "proxy-authorization", "te", "trailer",
			"transfer-encoding", "upgrade");

	private final RouteTable routeTable;

	private final RestClient client = RestClient.builder().build();

	public ApiProxyFilter(GatewayProperties properties) {
		this.routeTable = RouteTable.from(properties);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		Optional<GatewayRoute> route = routeTable.resolve(path);
		if (route.isEmpty()) {
			chain.doFilter(request, response);
			return;
		}
		URI uri = route.get().resolveUri(path, request.getQueryString());
		HttpMethod method = HttpMethod.valueOf(request.getMethod());
		byte[] body = org.springframework.util.StreamUtils.copyToByteArray(request.getInputStream());

		try {
			client.method(method)
					.uri(uri)
					.headers(headers -> copyRequestHeaders(request, headers))
					.body(body)
					.exchange((upstreamRequest, upstreamResponse) -> {
						response.setStatus(upstreamResponse.getStatusCode().value());
						upstreamResponse.getHeaders().forEach((name, values) -> {
							if (HOP_BY_HOP.contains(name.toLowerCase(java.util.Locale.ROOT))) {
								return;
							}
							values.forEach(value -> response.addHeader(name, value));
						});
						if (upstreamResponse.getBody() != null) {
							response.getOutputStream().write(upstreamResponse.getBody().readAllBytes());
						}
						return null;
					});
		}
		catch (RuntimeException failure) {
			response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.getWriter().write("{\"title\":\"Servicio no disponible\",\"status\":502,"
					+ "\"detail\":\"El gateway no pudo alcanzar el servicio de destino\"}");
		}
	}

	private void copyRequestHeaders(HttpServletRequest request, org.springframework.http.HttpHeaders target) {
		var names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String headerName = names.nextElement();
			String name = headerName.toLowerCase(java.util.Locale.ROOT);
			if (HOP_BY_HOP.contains(name)) {
				continue;
			}
			List<String> values = java.util.Collections.list(request.getHeaders(headerName));
			target.put(headerName, new java.util.ArrayList<>(values));
		}
	}

}

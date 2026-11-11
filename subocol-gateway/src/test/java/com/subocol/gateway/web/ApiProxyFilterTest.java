package com.subocol.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.subocol.gateway.config.GatewayProperties;
import com.subocol.gateway.routing.RouteTable;
import com.sun.net.httpserver.HttpServer;

import jakarta.servlet.FilterChain;

/**
 * Pruebas del reenvio de las peticiones de la API hacia el servicio de destino.
 */
class ApiProxyFilterTest {

	/** Servidor local que hace de destino y anota lo que recibe. */
	private static final class Destination {

		private final HttpServer server;

		private final AtomicReference<String> method = new AtomicReference<>();

		private final AtomicReference<String> path = new AtomicReference<>();

		private final AtomicReference<String> query = new AtomicReference<>();

		private final AtomicReference<String> body = new AtomicReference<>();

		private final AtomicReference<String> correlationId = new AtomicReference<>();

		private Destination() throws IOException {
			this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			this.server.createContext("/", exchange -> {
				this.method.set(exchange.getRequestMethod());
				this.path.set(exchange.getRequestURI().getPath());
				this.query.set(exchange.getRequestURI().getQuery());
				this.correlationId.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
				this.body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
				byte[] answer = "{\"orderId\":\"3f9d\"}".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(201, answer.length);
				exchange.getResponseBody().write(answer);
				exchange.close();
			});
			this.server.start();
		}

		private String url() {
			return "http://127.0.0.1:" + this.server.getAddress().getPort();
		}

		private void stop() {
			this.server.stop(0);
		}

	}

	private Destination orders;

	@AfterEach
	void stopDestinations() {
		if (this.orders != null) {
			this.orders.stop();
		}
	}

	private static GatewayProperties properties(String ordersTarget) {
		GatewayProperties properties = new GatewayProperties();
		GatewayProperties.Route route = new GatewayProperties.Route();
		route.setPath("/api/orders/**");
		route.setTarget(ordersTarget);
		properties.setRoutes(List.of(route));
		return properties;
	}

	@Test
	@DisplayName("reenvia la peticion al servicio de destino de la ruta que corresponde")
	void forwardsToTheMatchingDestination() throws Exception {
		this.orders = new Destination();
		ApiProxyFilter filter = new ApiProxyFilter(properties(this.orders.url()));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
		request.setQueryString("page=0&size=20");
		request.setContent("{\"customerName\":\"Ana Ruiz\"}".getBytes(StandardCharsets.UTF_8));
		request.addHeader("Content-Type", "application/json");
		request.addHeader("X-Correlation-Id", "abc-123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		// El camino, la consulta, el metodo, el cuerpo y las cabeceras viajan sin cambios.
		assertThat(this.orders.method.get()).isEqualTo("POST");
		assertThat(this.orders.path.get()).isEqualTo("/api/orders");
		assertThat(this.orders.query.get()).isEqualTo("page=0&size=20");
		assertThat(this.orders.body.get()).isEqualTo("{\"customerName\":\"Ana Ruiz\"}");
		assertThat(this.orders.correlationId.get()).isEqualTo("abc-123");

		// La respuesta del destino se devuelve sin transformar.
		assertThat(response.getStatus()).isEqualTo(201);
		assertThat(response.getContentAsString()).isEqualTo("{\"orderId\":\"3f9d\"}");
		assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
		// La peticion no debe seguir hacia la cadena de filtros: la respondio el gateway.
		verify(chain, never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("deja pasar intactas las peticiones que no tienen ruta")
	void passesThroughRequestsWithoutRoute() throws Exception {
		this.orders = new Destination();
		ApiProxyFilter filter = new ApiProxyFilter(properties(this.orders.url()));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/otra/ruta");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(any(), any());
		assertThat(this.orders.method.get()).isNull();
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEmpty();
	}

	@Test
	@DisplayName("devuelve 502 cuando el destino no responde")
	void returnsBadGatewayWhenDestinationIsUnreachable() throws Exception {
		// Un puerto donde no escucha nadie: la conexion falla de inmediato.
		int closedPort;
		try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
			closedPort = socket.getLocalPort();
		}
		ApiProxyFilter filter = new ApiProxyFilter(properties("http://127.0.0.1:" + closedPort));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(502);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		assertThat(response.getContentAsString()).contains("\"status\":502");
		verify(chain, never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("la tabla de rutas envia las ordenes al servicio de ordenes")
	void routeTableSendsOrdersToOrderService() {
		RouteTable routeTable = RouteTable.from(properties("http://order-service:8082"));

		assertThat(routeTable.resolve("/api/orders/3f9d")).isPresent();
		assertThat(routeTable.resolve("/api/orders/3f9d").orElseThrow().resolveUri("/api/orders/3f9d", "page=0"))
				.hasToString("http://order-service:8082/api/orders/3f9d?page=0");
		assertThat(routeTable.resolve("/api/otro")).isEmpty();
	}

}

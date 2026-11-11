package com.subocol.gateway.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.subocol.gateway.config.GatewayProperties;

/**
 * Pruebas de la logica de enrutado: que camino corresponde a que servicio.
 */
class RouteTableTest {

	private static GatewayProperties properties(String... paths) {
		GatewayProperties properties = new GatewayProperties();
		for (int index = 0; index < paths.length; index++) {
			GatewayProperties.Route route = new GatewayProperties.Route();
			route.setPath(paths[index]);
			route.setTarget("http://destino-" + index + ":8082");
			properties.getRoutes().add(route);
		}
		return properties;
	}

	@Test
	@DisplayName("el prefijo con comodin cubre el recurso y sus subrutas")
	void prefixPatternMatchesResourceAndSubpaths() {
		RouteMatcher matcher = RouteMatcher.compile("/api/orders/**");

		assertThat(matcher.matches("/api/orders")).isTrue();
		assertThat(matcher.matches("/api/orders/3f9d")).isTrue();
		assertThat(matcher.getPattern()).isEqualTo("/api/orders/**");
	}

	@Test
	@DisplayName("el prefijo respeta el limite de segmento")
	void prefixPatternRespectsSegmentBoundary() {
		RouteMatcher matcher = RouteMatcher.compile("/api/orders/**");

		assertThat(matcher.matches("/api/orders-legacy")).isFalse();
		assertThat(matcher.matches("/api/otro")).isFalse();
		assertThat(matcher.matches(null)).isFalse();
	}

	@Test
	@DisplayName("un patron exacto solo cubre ese recurso")
	void exactPatternMatchesOnlyThatPath() {
		RouteMatcher matcher = RouteMatcher.compile("/api/orders");

		assertThat(matcher.matches("/api/orders")).isTrue();
		assertThat(matcher.matches("/api/orders/3f9d")).isFalse();
	}

	@Test
	@DisplayName("un patron invalido se rechaza al compilarlo")
	void rejectsInvalidPatterns() {
		assertThatIllegalArgumentException().isThrownBy(() -> RouteMatcher.compile(null));
		assertThatIllegalArgumentException().isThrownBy(() -> RouteMatcher.compile("  "));
		assertThatIllegalArgumentException().isThrownBy(() -> RouteMatcher.compile("api/orders"));
	}

	@Test
	@DisplayName("el patron se normaliza y se puede inspeccionar")
	void normalizesAndDescribesPattern() {
		assertThat(RouteMatcher.compile("  /api/orders/  ").getPattern()).isEqualTo("/api/orders");
		assertThat(RouteMatcher.compile("/**").matches("/api/cualquiera")).isTrue();
		assertThat(RouteMatcher.compile("/api/orders/**").toString()).isEqualTo("RouteMatcher[/api/orders/**]");
	}

	@Test
	@DisplayName("la primera ruta que coincide gana")
	void firstMatchingRouteWins() {
		RouteTable routeTable = RouteTable.from(properties("/api/orders/**", "/api/appointments/**"));

		assertThat(routeTable.resolve("/api/orders/3f9d").orElseThrow().target())
				.hasToString("http://destino-0:8082");
		assertThat(routeTable.resolve("/api/appointments/7").orElseThrow().target())
				.hasToString("http://destino-1:8082");
		assertThat(routeTable.resolve("/api/otro")).isEmpty();
		assertThat(routeTable.getRoutes()).hasSize(2);
	}

	@Test
	@DisplayName("la direccion del destino conserva camino y cadena de consulta")
	void resolvedUriKeepsPathAndQuery() {
		GatewayRoute route = RouteTable.from(properties("/api/orders/**")).resolve("/api/orders/3f9d").orElseThrow();

		assertThat(route.resolveUri("/api/orders/3f9d", "page=0").toString())
				.isEqualTo("http://destino-0:8082/api/orders/3f9d?page=0");
		assertThat(route.resolveUri("/api/orders/3f9d", null).toString())
				.isEqualTo("http://destino-0:8082/api/orders/3f9d");
		assertThat(route.resolveUri("api/orders", "page=0").toString())
				.isEqualTo("http://destino-0:8082/api/orders?page=0");
	}

	@Test
	@DisplayName("la tabla de rutas es inmutable")
	void routeTableIsImmutable() {
		RouteTable routeTable = RouteTable.from(properties("/api/orders/**"));

		assertThat(routeTable.getRoutes()).isUnmodifiable();
		assertThat(List.copyOf(routeTable.getRoutes())).hasSize(1);
	}

}

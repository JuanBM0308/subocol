package com.subocol.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.subocol.gateway.config.GatewayProperties;
import com.subocol.gateway.routing.RouteTable;

/**
 * Comprobacion de que el gateway arranca con la configuracion real del repositorio.
 */
@SpringBootTest
class GatewayApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private GatewayProperties properties;

	@Test
	@DisplayName("el contexto de la aplicacion arranca con la tabla de rutas declarada")
	void contextLoadsWithConfiguredRoutes() {
		assertThat(this.context).isNotNull();
		assertThat(this.properties.getRoutes()).hasSize(2);
		// La tabla de rutas se compila desde la configuracion, igual que hace el filtro.
		RouteTable routeTable = RouteTable.from(this.properties);
		assertThat(routeTable.resolve("/api/orders/3f9d")).isPresent();
		assertThat(routeTable.resolve("/api/appointments/7")).isPresent();
		assertThat(routeTable.resolve("/api/otro")).isEmpty();
	}

	@Test
	@DisplayName("los filtros que gobiernan el trafico quedan registrados")
	void trafficFiltersAreRegistered() {
		assertThat(this.context.getBean(
				com.subocol.gateway.web.ApiProxyFilter.class)).isNotNull();
		assertThat(this.context.getBean(
				com.subocol.gateway.web.CorsPolicyFilter.class)).isNotNull();
		assertThat(this.context.getBean(
				com.subocol.gateway.web.CorrelationIdPropagationFilter.class)).isNotNull();
	}

}

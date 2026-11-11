package com.subocol.gateway.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.subocol.gateway.config.GatewayProperties;

/**
 * The route table of the gateway, built from the configuration file.
 *
 * <p>
 * The table is immutable: it is built once at start-up from {@code gateway.routes} and never
 * changes while the gateway is running.
 */
public final class RouteTable {

	private final List<GatewayRoute> routes;

	public RouteTable(List<GatewayRoute> routes) {
		this.routes = List.copyOf(routes);
	}

	/**
	 * Builds the route table from the configuration.
	 *
	 * @param properties the gateway configuration
	 * @return the compiled route table
	 */
	public static RouteTable from(GatewayProperties properties) {
		List<GatewayRoute> routes = new ArrayList<>();
		for (GatewayProperties.Route route : properties.getRoutes()) {
			routes.add(GatewayRoute.from(route));
		}
		return new RouteTable(routes);
	}

	/**
	 * Resolves the route that handles the given request path.
	 *
	 * @param path the request path, without the query string
	 * @return the matching route, or empty when no route handles the path
	 */
	public Optional<GatewayRoute> resolve(String path) {
		return routes.stream().filter(route -> route.matches(path)).findFirst();
	}

	/** @return every configured route, in declaration order */
	public List<GatewayRoute> getRoutes() {
		return routes;
	}

}

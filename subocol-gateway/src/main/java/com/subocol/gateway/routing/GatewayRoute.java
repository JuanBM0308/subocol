package com.subocol.gateway.routing;

import java.net.URI;

import com.subocol.gateway.config.GatewayProperties;

/**
 * One entry of the route table: a path pattern and the destination service that answers it.
 *
 * @param pattern the normalized path pattern
 * @param target the absolute base address of the destination service
 * @param matcher the compiled matcher of the pattern
 */
public record GatewayRoute(String pattern, URI target, RouteMatcher matcher) {

	/**
	 * Builds a route from its configuration entry.
	 *
	 * @param route the configured route
	 * @return the compiled route
	 */
	public static GatewayRoute from(GatewayProperties.Route route) {
		RouteMatcher matcher = RouteMatcher.compile(route.getPath());
		return new GatewayRoute(matcher.getPattern(), URI.create(route.getTarget()), matcher);
	}

	/**
	 * Tells whether this route handles the given request path.
	 *
	 * @param path the request path, without the query string
	 * @return {@code true} when the path belongs to this route
	 */
	public boolean matches(String path) {
		return matcher.matches(path);
	}

	/**
	 * Builds the address of the destination service for the given request.
	 *
	 * <p>
	 * The path and the query string are forwarded untouched: the gateway does not rewrite the
	 * resource, it only changes the host.
	 *
	 * @param requestPath the request path
	 * @param queryString the query string, may be {@code null}
	 * @return the absolute address of the destination service
	 */
	public URI resolveUri(String requestPath, String queryString) {
		StringBuilder address = new StringBuilder(target.toString());
		if (!requestPath.startsWith("/")) {
			address.append('/');
		}
		address.append(requestPath);
		if (queryString != null && !queryString.isBlank()) {
			address.append('?').append(queryString);
		}
		return URI.create(address.toString());
	}

}

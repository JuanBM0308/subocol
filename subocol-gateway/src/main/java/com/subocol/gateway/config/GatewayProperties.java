package com.subocol.gateway.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration of the gateway, bound from the {@code gateway.*} block of {@code application.yaml}.
 *
 * <p>
 * The route table lives in the configuration file on purpose: the Java code must not know the
 * address of any destination service.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

	/** Route table. The first route whose pattern matches the request path wins. */
	private List<Route> routes = new ArrayList<>();

	/** Cross-origin policy applied to every request that carries an {@code Origin} header. */
	private Cors cors = new Cors();

	/** Timeouts applied to the outgoing calls. */
	private Timeouts timeouts = new Timeouts();

	/** A single entry of the route table. */
	@Getter
	@Setter
	public static class Route {

		/** Path pattern handled by this route, for example {@code /api/orders/**}. */
		private String path;

		/** Absolute base address of the destination service, for example {@code http://order-service:8082}. */
		private String target;

	}

	/** Cross-origin policy. Wildcards are rejected on purpose. */
	@Getter
	@Setter
	public static class Cors {

		/** The only origins accepted by the gateway. */
		private List<String> allowedOrigins = new ArrayList<>();

	}

	/** Timeouts applied to the outgoing calls. */
	@Getter
	@Setter
	public static class Timeouts {

		/** Maximum time to establish the connection with the destination service. */
		private Duration connect = Duration.ofSeconds(5);

		/** Maximum time to wait for the response of the destination service. */
		private Duration response = Duration.ofSeconds(10);

	}

}

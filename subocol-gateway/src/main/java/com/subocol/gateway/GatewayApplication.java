package com.subocol.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the Taller 360 gateway.
 *
 * <p>
 * The gateway is a minimal reverse proxy in front of the workshop services: it routes the
 * {@code /api/**} requests to the destination declared in the route table and applies the
 * cross-origin and correlation-id policies of the workshop.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}

package com.subocol.gateway.client;

/**
 * Port used by the gateway to call the destination services.
 *
 * <p>
 * The proxy filter only depends on this interface, so the HTTP client can be replaced or
 * replaced by a test double without touching the routing logic.
 */
public interface UpstreamClient {

	/**
	 * Forwards a request to a destination service.
	 *
	 * @param request the request to forward
	 * @return the response of the destination service
	 * @throws RuntimeException when the destination service cannot be reached or does not answer
	 *         in time
	 */
	UpstreamResponse exchange(UpstreamRequest request);

}

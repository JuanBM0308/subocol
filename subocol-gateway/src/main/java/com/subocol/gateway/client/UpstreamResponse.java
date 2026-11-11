package com.subocol.gateway.client;

import org.springframework.http.HttpHeaders;

/**
 * The response returned by a destination service.
 *
 * @param status the HTTP status code
 * @param headers the headers returned by the destination service
 * @param body the body returned by the destination service
 */
public record UpstreamResponse(int status, HttpHeaders headers, byte[] body) {
}

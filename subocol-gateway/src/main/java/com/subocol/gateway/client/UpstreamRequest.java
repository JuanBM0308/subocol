package com.subocol.gateway.client;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * A request forwarded to a destination service.
 *
 * @param method the HTTP method
 * @param uri the absolute address of the destination service
 * @param headers the headers to forward
 * @param body the body to forward, never rewritten by the gateway
 */
public record UpstreamRequest(HttpMethod method, URI uri, HttpHeaders headers, byte[] body) {
}

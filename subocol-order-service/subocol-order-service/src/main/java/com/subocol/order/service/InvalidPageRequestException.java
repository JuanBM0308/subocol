package com.subocol.order.service;

/**
 * Error de negocio: la paginación solicitada no es válida.
 */
public class InvalidPageRequestException extends RuntimeException {

	public InvalidPageRequestException(String message) {
		super(message);
	}

}

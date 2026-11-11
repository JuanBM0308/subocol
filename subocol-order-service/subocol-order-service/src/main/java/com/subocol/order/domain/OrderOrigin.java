package com.subocol.order.domain;

/**
 * Origen de la orden.
 *
 * <p>El origen lo fija siempre el servidor: el cliente no puede elegirlo. En el
 * MVP la única vía de registro es la de un asesor, por lo que toda orden nace
 * como {@link #ADVISOR}.
 */
public enum OrderOrigin {

	ADVISOR,
	INSURER

}

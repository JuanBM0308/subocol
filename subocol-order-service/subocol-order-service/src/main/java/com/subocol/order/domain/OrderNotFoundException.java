package com.subocol.order.domain;

import java.util.UUID;

/**
 * Error de negocio: la orden solicitada no existe en {@code orders_db}.
 */
public class OrderNotFoundException extends RuntimeException {

	private final UUID orderId;

	public OrderNotFoundException(UUID orderId) {
		super("Order not found: " + orderId);
		this.orderId = orderId;
	}

	public UUID getOrderId() {
		return this.orderId;
	}

}

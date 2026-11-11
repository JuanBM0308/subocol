package com.subocol.order.api.dto;

import java.util.UUID;

import com.subocol.order.domain.OrderStatus;

/**
 * Respuesta del registro de una orden.
 *
 * @param orderId identificador de la orden
 * @param status estado inicial de la orden
 * @param createdAt fecha de registro en ISO-8601 UTC con milisegundos
 */
public record OrderCreatedResponse(UUID orderId, OrderStatus status, String createdAt) {

}

package com.subocol.order.api.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.subocol.order.domain.OrderStatus;

/**
 * Orden de servicio.
 *
 * <p>En el listado la orden no trae historial; en el detalle sí. El campo
 * {@code history} se omite cuando no aplica, de modo que la respuesta del
 * listado es exactamente la del contrato.
 *
 * @param orderId identificador de la orden
 * @param customerName nombre del cliente
 * @param vehiclePlate matrícula del vehículo
 * @param vehicleDescription descripción del vehículo
 * @param insurerCode código de la aseguradora, puede ser {@code null}
 * @param status estado de la orden
 * @param createdAt fecha de registro en ISO-8601 UTC con milisegundos
 * @param history historial de estados, solo en el detalle
 */
public record OrderResponse(UUID orderId, String customerName, String vehiclePlate, String vehicleDescription,
		String insurerCode, OrderStatus status, String createdAt,

		@JsonInclude(JsonInclude.Include.NON_NULL) List<OrderHistoryEntry> history) {

}

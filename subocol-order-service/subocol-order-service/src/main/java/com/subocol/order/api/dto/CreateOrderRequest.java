package com.subocol.order.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que el cliente envía para registrar una orden.
 *
 * <p>El origen de la orden no aparece aquí: lo fija el servidor.
 *
 * @param customerName nombre del cliente, obligatorio, máximo 120 caracteres
 * @param vehiclePlate matrícula del vehículo, obligatoria, máximo 16 caracteres
 * @param vehicleDescription descripción del vehículo, obligatoria, máximo 2000
 *        caracteres
 * @param insurerCode código de la aseguradora, opcional, máximo 32 caracteres
 */
public record CreateOrderRequest(

		@NotBlank(message = "customerName is required") @Size(max = 120, message = "customerName must be at most 120 characters") String customerName,

		@NotBlank(message = "vehiclePlate is required") @Size(max = 16, message = "vehiclePlate must be at most 16 characters") String vehiclePlate,

		@NotBlank(message = "vehicleDescription is required") @Size(max = 2000, message = "vehicleDescription must be at most 2000 characters") String vehicleDescription,

		@Size(max = 32, message = "insurerCode must be at most 32 characters") String insurerCode) {

}

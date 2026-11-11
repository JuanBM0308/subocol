package com.subocol.order.domain;

/**
 * Estados de una orden de servicio.
 *
 * <p>El MVP del contrato solo crea órdenes en {@link #RECIBIDA}; el esquema
 * deja espacio ({@code varchar(32)}) para los estados siguientes del flujo
 * cuando entren en alcance.
 */
public enum OrderStatus {

	RECIBIDA

}

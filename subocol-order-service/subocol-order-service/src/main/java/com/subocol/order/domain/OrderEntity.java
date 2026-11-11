package com.subocol.order.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Orden de servicio registrada en el taller.
 *
 * <p>Mapea la tabla {@code orders} de {@code orders_db}, que es propiedad
 * exclusiva de este servicio.
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "customer_name", nullable = false, length = 120)
	private String customerName;

	@Column(name = "vehicle_plate", nullable = false, length = 16)
	private String vehiclePlate;

	@Column(name = "vehicle_description", nullable = false, length = 2000)
	private String vehicleDescription;

	@Column(name = "insurer_code", length = 32)
	private String insurerCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private OrderStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/**
	 * Origen de la orden. No se persiste: el esquema del MVP no tiene columna
	 * para él, pero el agregado lo conoce porque el servidor lo fija.
	 */
	@Transient
	private OrderOrigin origin;

	public UUID getId() {
		return this.id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getCustomerName() {
		return this.customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getVehiclePlate() {
		return this.vehiclePlate;
	}

	public void setVehiclePlate(String vehiclePlate) {
		this.vehiclePlate = vehiclePlate;
	}

	public String getVehicleDescription() {
		return this.vehicleDescription;
	}

	public void setVehicleDescription(String vehicleDescription) {
		this.vehicleDescription = vehicleDescription;
	}

	public String getInsurerCode() {
		return this.insurerCode;
	}

	public void setInsurerCode(String insurerCode) {
		this.insurerCode = insurerCode;
	}

	public OrderStatus getStatus() {
		return this.status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public OrderOrigin getOrigin() {
		return this.origin;
	}

	public void setOrigin(OrderOrigin origin) {
		this.origin = origin;
	}

}

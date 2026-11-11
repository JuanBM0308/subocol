package com.subocol.order.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Renglón del historial de estados de una orden.
 *
 * <p>Mapea la tabla {@code order_status_history} de {@code orders_db}, que es
 * estrictamente <em>append-only</em>: el historial no se actualiza ni se
 * borra, y cada renglón encadena su hash con el del renglón anterior.
 */
@Entity
@Table(name = "order_status_history")
public class OrderStatusHistoryEntity {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "event_id", nullable = false, unique = true)
	private UUID eventId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_order_status_history_order"))
	private OrderEntity order;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 32)
	private OrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 32)
	private OrderStatus toStatus;

	@Column(name = "reason", length = 500)
	private String reason;

	@Column(name = "changed_by", nullable = false, length = 120)
	private String changedBy;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "event_hash", nullable = false, length = 64)
	private String eventHash;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "previous_hash", nullable = false, length = 64)
	private String previousHash;

	public UUID getId() {
		return this.id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getEventId() {
		return this.eventId;
	}

	public void setEventId(UUID eventId) {
		this.eventId = eventId;
	}

	public OrderEntity getOrder() {
		return this.order;
	}

	public void setOrder(OrderEntity order) {
		this.order = order;
	}

	public OrderStatus getFromStatus() {
		return this.fromStatus;
	}

	public void setFromStatus(OrderStatus fromStatus) {
		this.fromStatus = fromStatus;
	}

	public OrderStatus getToStatus() {
		return this.toStatus;
	}

	public void setToStatus(OrderStatus toStatus) {
		this.toStatus = toStatus;
	}

	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getChangedBy() {
		return this.changedBy;
	}

	public void setChangedBy(String changedBy) {
		this.changedBy = changedBy;
	}

	public Instant getChangedAt() {
		return this.changedAt;
	}

	public void setChangedAt(Instant changedAt) {
		this.changedAt = changedAt;
	}

	public String getEventHash() {
		return this.eventHash;
	}

	public void setEventHash(String eventHash) {
		this.eventHash = eventHash;
	}

	public String getPreviousHash() {
		return this.previousHash;
	}

	public void setPreviousHash(String previousHash) {
		this.previousHash = previousHash;
	}

}

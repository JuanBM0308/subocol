package com.subocol.order.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subocol.order.api.dto.CreateOrderRequest;
import com.subocol.order.api.dto.OrderCreatedResponse;
import com.subocol.order.api.dto.OrderHistoryEntry;
import com.subocol.order.api.dto.OrderPageResponse;
import com.subocol.order.api.dto.OrderResponse;
import com.subocol.order.domain.OrderEntity;
import com.subocol.order.domain.OrderNotFoundException;
import com.subocol.order.domain.OrderOrigin;
import com.subocol.order.domain.OrderStatus;
import com.subocol.order.domain.OrderStatusHistoryEntity;
import com.subocol.order.repository.OrderRepository;
import com.subocol.order.repository.OrderStatusHistoryRepository;
import com.subocol.order.support.Iso8601;

/**
 * Reglas de negocio de las órdenes de servicio.
 *
 * <p>El registro de una orden y la primera fila de su historial se escriben en
 * la misma transacción: o quedan las dos, o no queda ninguna.
 */
@Service
public class OrderService {

	/**
	 * El origen lo fija el servidor; el cliente no puede elegirlo.
	 */
	private static final OrderOrigin SERVER_ORIGIN = OrderOrigin.ADVISOR;

	private static final OrderStatus INITIAL_STATUS = OrderStatus.RECIBIDA;

	private static final String INITIAL_REASON = "Orden registrada";

	private static final int MAX_PAGE_SIZE = 100;

	private final OrderRepository orderRepository;

	private final OrderStatusHistoryRepository historyRepository;

	private final HistoryHashService historyHashService;

	private final String actor;

	public OrderService(OrderRepository orderRepository, OrderStatusHistoryRepository historyRepository,
			HistoryHashService historyHashService, @Value("${app.actor}") String actor) {
		this.orderRepository = orderRepository;
		this.historyRepository = historyRepository;
		this.historyHashService = historyHashService;
		this.actor = actor;
	}

	@Transactional
	public OrderCreatedResponse create(CreateOrderRequest request) {
		Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		OrderEntity order = new OrderEntity();
		order.setId(UUID.randomUUID());
		order.setCustomerName(trim(request.customerName()));
		order.setVehiclePlate(trim(request.vehiclePlate()));
		order.setVehicleDescription(trim(request.vehicleDescription()));
		order.setInsurerCode(trimToNull(request.insurerCode()));
		order.setStatus(INITIAL_STATUS);
		order.setCreatedAt(createdAt);
		order.setOrigin(SERVER_ORIGIN);
		OrderEntity savedOrder = orderRepository.save(order);

		UUID eventId = UUID.randomUUID();
		String previousHash = HistoryHashService.GENESIS_HASH;
		String eventHash = historyHashService.compute(eventId, savedOrder.getId(), null, INITIAL_STATUS.name(),
				INITIAL_REASON, actor, createdAt, previousHash);

		OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
		history.setId(UUID.randomUUID());
		history.setEventId(eventId);
		history.setOrder(savedOrder);
		history.setFromStatus(null);
		history.setToStatus(INITIAL_STATUS);
		history.setReason(INITIAL_REASON);
		history.setChangedBy(actor);
		history.setChangedAt(createdAt);
		history.setEventHash(eventHash);
		history.setPreviousHash(previousHash);
		historyRepository.save(history);

		return new OrderCreatedResponse(savedOrder.getId(), savedOrder.getStatus(),
				Iso8601.format(savedOrder.getCreatedAt()));
	}

	@Transactional(readOnly = true)
	public OrderPageResponse list(int page, int size) {
		if (page < 0) {
			throw new InvalidPageRequestException("page must be greater than or equal to zero");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new InvalidPageRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
		}

		Pageable pageable = PageRequest.of(page, size,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		Page<OrderEntity> orders = orderRepository.findAll(pageable);
		List<OrderResponse> content = orders.getContent().stream().map(this::toOrderResponse).toList();
		return new OrderPageResponse(content, orders.getNumber(), orders.getSize(), orders.getTotalElements());
	}

	@Transactional(readOnly = true)
	public OrderResponse detail(UUID orderId) {
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));
		List<OrderHistoryEntry> history = historyRepository.findByOrder_IdOrderByChangedAtAsc(orderId).stream()
				.map(this::toHistoryEntry)
				.toList();
		return toOrderResponse(order, history);
	}

	private OrderResponse toOrderResponse(OrderEntity order) {
		return toOrderResponse(order, null);
	}

	private OrderResponse toOrderResponse(OrderEntity order, List<OrderHistoryEntry> history) {
		return new OrderResponse(order.getId(), order.getCustomerName(), order.getVehiclePlate(),
				order.getVehicleDescription(), order.getInsurerCode(), order.getStatus(),
				Iso8601.format(order.getCreatedAt()), history);
	}

	private OrderHistoryEntry toHistoryEntry(OrderStatusHistoryEntity history) {
		return new OrderHistoryEntry(history.getEventId(), history.getFromStatus(), history.getToStatus(),
				history.getReason(), history.getChangedBy(), Iso8601.format(history.getChangedAt()),
				history.getEventHash(), history.getPreviousHash());
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}

	private static String trimToNull(String value) {
		String trimmed = trim(value);
		return (trimmed == null || trimmed.isEmpty()) ? null : trimmed;
	}

}

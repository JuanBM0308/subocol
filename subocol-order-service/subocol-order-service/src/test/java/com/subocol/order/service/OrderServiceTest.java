package com.subocol.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	private static final String ACTOR = "dev-advisor";

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderStatusHistoryRepository historyRepository;

	@Captor
	private ArgumentCaptor<OrderEntity> orderCaptor;

	@Captor
	private ArgumentCaptor<OrderStatusHistoryEntity> historyCaptor;

	@Captor
	private ArgumentCaptor<Pageable> pageableCaptor;

	private HistoryHashService historyHashService;

	private OrderService orderService;

	@BeforeEach
	void setUp() {
		this.historyHashService = new HistoryHashService();
		this.orderService = new OrderService(orderRepository, historyRepository, historyHashService, ACTOR);
	}

	@Test
	@DisplayName("Registra la orden y su primera fila de historial con el hash encadenado")
	void registersTheOrderAndItsFirstHistoryEntry() {
		when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(historyRepository.save(any(OrderStatusHistoryEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		OrderCreatedResponse response = orderService.create(new CreateOrderRequest("Ana Ruiz", "ABC123",
				"Toyota Corolla 2018", null));

		verify(orderRepository).save(orderCaptor.capture());
		OrderEntity order = orderCaptor.getValue();
		assertThat(order.getId()).isNotNull();
		assertThat(order.getStatus()).isEqualTo(OrderStatus.RECIBIDA);
		assertThat(order.getOrigin()).isEqualTo(OrderOrigin.ADVISOR);
		assertThat(order.getCustomerName()).isEqualTo("Ana Ruiz");
		assertThat(order.getVehiclePlate()).isEqualTo("ABC123");
		assertThat(order.getVehicleDescription()).isEqualTo("Toyota Corolla 2018");
		assertThat(order.getInsurerCode()).isNull();
		assertThat(order.getCreatedAt()).isNotNull();

		verify(historyRepository).save(historyCaptor.capture());
		OrderStatusHistoryEntity history = historyCaptor.getValue();
		assertThat(history.getOrder()).isSameAs(order);
		assertThat(history.getEventId()).isNotNull();
		assertThat(history.getFromStatus()).isNull();
		assertThat(history.getToStatus()).isEqualTo(OrderStatus.RECIBIDA);
		assertThat(history.getReason()).isEqualTo("Orden registrada");
		assertThat(history.getChangedBy()).isEqualTo(ACTOR);
		assertThat(history.getChangedAt()).isEqualTo(order.getCreatedAt());
		assertThat(history.getPreviousHash()).isEqualTo(HistoryHashService.GENESIS_HASH);
		assertThat(history.getEventHash()).isEqualTo(historyHashService.compute(history.getEventId(), order.getId(),
				null, OrderStatus.RECIBIDA.name(), "Orden registrada", ACTOR, history.getChangedAt(),
				HistoryHashService.GENESIS_HASH));

		assertThat(response.orderId()).isEqualTo(order.getId());
		assertThat(response.status()).isEqualTo(OrderStatus.RECIBIDA);
		assertThat(response.createdAt()).isEqualTo(Iso8601.format(order.getCreatedAt()));
	}

	@Test
	@DisplayName("Normaliza los datos que envía el cliente")
	void normalizesTheClientValues() {
		when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(historyRepository.save(any(OrderStatusHistoryEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		orderService.create(new CreateOrderRequest("  Ana Ruiz  ", " ABC123 ", " Toyota Corolla 2018 ", "  "));

		verify(orderRepository).save(orderCaptor.capture());
		OrderEntity order = orderCaptor.getValue();
		assertThat(order.getCustomerName()).isEqualTo("Ana Ruiz");
		assertThat(order.getVehiclePlate()).isEqualTo("ABC123");
		assertThat(order.getVehicleDescription()).isEqualTo("Toyota Corolla 2018");
		assertThat(order.getInsurerCode()).isNull();
	}

	@Test
	@DisplayName("Lista las órdenes de la más reciente a la más antigua")
	void listsOrdersNewestFirst() {
		OrderEntity order = buildOrder();
		when(orderRepository.findAll(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1));

		OrderPageResponse page = orderService.list(0, 20);

		verify(orderRepository).findAll(pageableCaptor.capture());
		Sort sort = pageableCaptor.getValue().getSort();
		assertThat(sort.getOrderFor("createdAt")).isNotNull();
		assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);

		assertThat(page.content()).hasSize(1);
		assertThat(page.page()).isZero();
		assertThat(page.size()).isEqualTo(20);
		assertThat(page.totalElements()).isEqualTo(1);
		OrderResponse response = page.content().get(0);
		assertThat(response.orderId()).isEqualTo(order.getId());
		assertThat(response.customerName()).isEqualTo("Ana Ruiz");
		assertThat(response.status()).isEqualTo(OrderStatus.RECIBIDA);
		assertThat(response.createdAt()).isEqualTo(Iso8601.format(order.getCreatedAt()));
		assertThat(response.history()).isNull();
	}

	@Test
	@DisplayName("Devuelve el detalle de la orden con su historial")
	void returnsTheOrderDetailWithItsHistory() {
		OrderEntity order = buildOrder();
		OrderStatusHistoryEntity history = buildHistory(order);
		when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
		when(historyRepository.findByOrder_IdOrderByChangedAtAsc(order.getId())).thenReturn(List.of(history));

		OrderResponse response = orderService.detail(order.getId());

		assertThat(response.orderId()).isEqualTo(order.getId());
		assertThat(response.vehiclePlate()).isEqualTo("ABC123");
		assertThat(response.vehicleDescription()).isEqualTo("Toyota Corolla 2018");
		assertThat(response.insurerCode()).isEqualTo("SUR-01");
		assertThat(response.status()).isEqualTo(OrderStatus.RECIBIDA);
		assertThat(response.history()).hasSize(1);

		OrderHistoryEntry entry = response.history().get(0);
		assertThat(entry.eventId()).isEqualTo(history.getEventId());
		assertThat(entry.fromStatus()).isNull();
		assertThat(entry.toStatus()).isEqualTo(OrderStatus.RECIBIDA);
		assertThat(entry.reason()).isEqualTo("Orden registrada");
		assertThat(entry.changedBy()).isEqualTo(ACTOR);
		assertThat(entry.changedAt()).isEqualTo(Iso8601.format(history.getChangedAt()));
		assertThat(entry.eventHash()).isEqualTo(history.getEventHash());
		assertThat(entry.previousHash()).isEqualTo(HistoryHashService.GENESIS_HASH);
	}

	@Test
	@DisplayName("Falla cuando la orden no existe")
	void failsWhenTheOrderDoesNotExist() {
		UUID missing = UUID.fromString("3f9d2f0e-5f0e-4a2f-9d5f-2f0a1b2c3d4e");
		when(orderRepository.findById(missing)).thenReturn(Optional.empty());

		OrderNotFoundException exception = catchThrowableOfType(() -> orderService.detail(missing),
				OrderNotFoundException.class);

		assertThat(exception).isNotNull();
		assertThat(exception.getOrderId()).isEqualTo(missing);
		assertThat(exception.getMessage()).contains(missing.toString());
	}

	@Test
	@DisplayName("Rechaza una paginación inválida")
	void rejectsAnInvalidPagination() {
		assertThatThrownBy(() -> orderService.list(-1, 20)).isInstanceOf(InvalidPageRequestException.class);
		assertThatThrownBy(() -> orderService.list(0, 0)).isInstanceOf(InvalidPageRequestException.class);
		assertThatThrownBy(() -> orderService.list(0, 101)).isInstanceOf(InvalidPageRequestException.class);
	}

	private static OrderEntity buildOrder() {
		OrderEntity order = new OrderEntity();
		order.setId(UUID.fromString("3f9d2f0e-5f0e-4a2f-9d5f-2f0a1b2c3d4e"));
		order.setCustomerName("Ana Ruiz");
		order.setVehiclePlate("ABC123");
		order.setVehicleDescription("Toyota Corolla 2018");
		order.setInsurerCode("SUR-01");
		order.setStatus(OrderStatus.RECIBIDA);
		order.setCreatedAt(Instant.parse("2026-11-11T15:04:05.123Z"));
		order.setOrigin(OrderOrigin.ADVISOR);
		return order;
	}

	private static OrderStatusHistoryEntity buildHistory(OrderEntity order) {
		UUID eventId = UUID.fromString("9c1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8");
		Instant changedAt = order.getCreatedAt();
		String previousHash = HistoryHashService.GENESIS_HASH;
		String eventHash = new HistoryHashService().compute(eventId, order.getId(), null, OrderStatus.RECIBIDA.name(),
				"Orden registrada", ACTOR, changedAt, previousHash);

		OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
		history.setId(UUID.fromString("b6f0a9c1-2d3e-4f50-8a6b-7c8d9e0f1a2b"));
		history.setEventId(eventId);
		history.setOrder(order);
		history.setFromStatus(null);
		history.setToStatus(OrderStatus.RECIBIDA);
		history.setReason("Orden registrada");
		history.setChangedBy(ACTOR);
		history.setChangedAt(changedAt);
		history.setEventHash(eventHash);
		history.setPreviousHash(previousHash);
		return history;
	}

}

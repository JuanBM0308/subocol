package com.subocol.order.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.subocol.order.api.dto.CreateOrderRequest;
import com.subocol.order.api.dto.OrderCreatedResponse;
import com.subocol.order.api.dto.OrderPageResponse;
import com.subocol.order.api.dto.OrderResponse;
import com.subocol.order.service.OrderService;

import jakarta.validation.Valid;

/**
 * API de órdenes de servicio.
 *
 * <p>El gateway expone estas mismas rutas bajo {@code /api/orders}.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	public ResponseEntity<OrderCreatedResponse> create(@Valid @RequestBody CreateOrderRequest request) {
		OrderCreatedResponse created = orderService.create(request);
		return ResponseEntity.created(URI.create("/api/orders/" + created.orderId())).body(created);
	}

	@GetMapping
	public OrderPageResponse list(@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		return orderService.list(page, size);
	}

	@GetMapping("/{orderId}")
	public OrderResponse detail(@PathVariable("orderId") UUID orderId) {
		return orderService.detail(orderId);
	}

}

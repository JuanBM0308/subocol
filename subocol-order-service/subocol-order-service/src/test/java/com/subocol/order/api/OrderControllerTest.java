package com.subocol.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.subocol.order.api.dto.CreateOrderRequest;
import com.subocol.order.api.dto.OrderCreatedResponse;
import com.subocol.order.api.dto.OrderHistoryEntry;
import com.subocol.order.api.dto.OrderPageResponse;
import com.subocol.order.api.dto.OrderResponse;
import com.subocol.order.domain.OrderNotFoundException;
import com.subocol.order.domain.OrderStatus;
import com.subocol.order.service.InvalidPageRequestException;
import com.subocol.order.service.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

	private static final UUID ORDER_ID = UUID.fromString("3f9d2f0e-5f0e-4a2f-9d5f-2f0a1b2c3d4e");

	private static final UUID EVENT_ID = UUID.fromString("9c1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8");

	private static final String CREATED_AT = "2026-11-11T15:04:05.123Z";

	private static final String EVENT_HASH = "9f2b3c4d5e6f708192a3b4c5d6e7f8091a2b3c4d5e6f708192a3b4c5d6e7f809";

	private static final String GENESIS_HASH = "0".repeat(64);

	@Mock
	private OrderService orderService;

	@Captor
	private ArgumentCaptor<CreateOrderRequest> requestCaptor;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		OrderController controller = new OrderController(orderService);
		this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	@DisplayName("Registra una orden y responde 201 con el identificador")
	void registersAnOrder() throws Exception {
		when(orderService.create(any(CreateOrderRequest.class)))
				.thenReturn(new OrderCreatedResponse(ORDER_ID, OrderStatus.RECIBIDA, CREATED_AT));

		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "customerName": "Ana Ruiz",
						  "vehiclePlate": "ABC123",
						  "vehicleDescription": "Toyota Corolla 2018",
						  "insurerCode": null
						}
						"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/orders/" + ORDER_ID))
			.andExpect(content().json("""
					{
					  "orderId": "3f9d2f0e-5f0e-4a2f-9d5f-2f0a1b2c3d4e",
					  "status": "RECIBIDA",
					  "createdAt": "2026-11-11T15:04:05.123Z"
					}
					""", true));

		verify(orderService).create(requestCaptor.capture());
		CreateOrderRequest request = requestCaptor.getValue();
		assertThat(request.customerName()).isEqualTo("Ana Ruiz");
		assertThat(request.vehiclePlate()).isEqualTo("ABC123");
		assertThat(request.vehicleDescription()).isEqualTo("Toyota Corolla 2018");
		assertThat(request.insurerCode()).isNull();
	}

	@Test
	@DisplayName("Rechaza una orden incompleta con ProblemDetail")
	void rejectsAnInvalidOrder() throws Exception {
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "vehiclePlate": "ABC123",
						  "vehicleDescription": "Toyota Corolla 2018"
						}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Validation failed"))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors[0].field").value("customerName"))
			.andExpect(jsonPath("$.trace").doesNotExist());
	}

	@Test
	@DisplayName("Devuelve el listado paginado de órdenes")
	void listsOrders() throws Exception {
		when(orderService.list(0, 20)).thenReturn(new OrderPageResponse(List.of(new OrderResponse(ORDER_ID,
				"Ana Ruiz", "ABC123", "Toyota Corolla 2018", null, OrderStatus.RECIBIDA, CREATED_AT, null)), 0, 20, 1));

		mockMvc.perform(get("/api/orders").param("page", "0").param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(content().json("""
					{
					  "content": [
					    {
					      "orderId": "3f9d2f0e-5f0e-4a2f-9d5f-2f0a1b2c3d4e",
					      "customerName": "Ana Ruiz",
					      "vehiclePlate": "ABC123",
					      "vehicleDescription": "Toyota Corolla 2018",
					      "insurerCode": null,
					      "status": "RECIBIDA",
					      "createdAt": "2026-11-11T15:04:05.123Z"
					    }
					  ],
					  "page": 0,
					  "size": 20,
					  "totalElements": 1
					}
					""", true));
	}

	@Test
	@DisplayName("Devuelve el detalle de la orden con su historial")
	void returnsTheOrderDetail() throws Exception {
		when(orderService.detail(ORDER_ID)).thenReturn(new OrderResponse(ORDER_ID, "Ana Ruiz", "ABC123",
				"Toyota Corolla 2018", null, OrderStatus.RECIBIDA, CREATED_AT,
				List.of(new OrderHistoryEntry(EVENT_ID, null, OrderStatus.RECIBIDA, "Orden registrada", "dev-advisor",
						CREATED_AT, EVENT_HASH, GENESIS_HASH))));

		mockMvc.perform(get("/api/orders/{orderId}", ORDER_ID))
			.andExpect(status().isOk())
			.andExpect(content().json("""
					{
					  "orderId": "3f9d2f0e-5f0e-4a2f-9d5f-2f0a1b2c3d4e",
					  "customerName": "Ana Ruiz",
					  "vehiclePlate": "ABC123",
					  "vehicleDescription": "Toyota Corolla 2018",
					  "insurerCode": null,
					  "status": "RECIBIDA",
					  "createdAt": "2026-11-11T15:04:05.123Z",
					  "history": [
					    {
					      "eventId": "9c1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8",
					      "fromStatus": null,
					      "toStatus": "RECIBIDA",
					      "reason": "Orden registrada",
					      "changedBy": "dev-advisor",
					      "changedAt": "2026-11-11T15:04:05.123Z",
					      "eventHash": "9f2b3c4d5e6f708192a3b4c5d6e7f8091a2b3c4d5e6f708192a3b4c5d6e7f809",
					      "previousHash": "0000000000000000000000000000000000000000000000000000000000000000"
					    }
					  ]
					}
					""", true));
	}

	@Test
	@DisplayName("Rechaza los campos que exceden la longitud máxima del contrato")
	void rejectsValuesOverTheMaximumLength() throws Exception {
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "customerName": "%s",
						  "vehiclePlate": "ABC123",
						  "vehicleDescription": "Toyota Corolla 2018"
						}
						""".formatted("a".repeat(121))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("customerName"));

		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "customerName": "Ana Ruiz",
						  "vehiclePlate": "%s",
						  "vehicleDescription": "Toyota Corolla 2018"
						}
						""".formatted("b".repeat(17))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("vehiclePlate"));

		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "customerName": "Ana Ruiz",
						  "vehiclePlate": "ABC123",
						  "vehicleDescription": "Toyota Corolla 2018",
						  "insurerCode": "%s"
						}
						""".formatted("c".repeat(33))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("insurerCode"));
	}

	@Test
	@DisplayName("Responde 400 cuando el cuerpo de la petición no es JSON válido")
	void respondsBadRequestForAMalformedBody() throws Exception {
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"customerName\": "))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Malformed request body"))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.trace").doesNotExist());
	}

	@Test
	@DisplayName("Responde 404 cuando la orden no existe")
	void respondsNotFoundWhenTheOrderDoesNotExist() throws Exception {
		when(orderService.detail(ORDER_ID)).thenThrow(new OrderNotFoundException(ORDER_ID));

		mockMvc.perform(get("/api/orders/{orderId}", ORDER_ID))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Order not found"))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
			.andExpect(jsonPath("$.trace").doesNotExist());
	}

	@Test
	@DisplayName("Responde 400 cuando la paginación no es válida")
	void respondsBadRequestForAnInvalidPagination() throws Exception {
		when(orderService.list(0, 500)).thenThrow(new InvalidPageRequestException("size must be between 1 and 100"));

		mockMvc.perform(get("/api/orders").param("page", "0").param("size", "500"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid pagination"))
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("Responde 400 cuando el identificador no es un UUID")
	void respondsBadRequestForAnInvalidIdentifier() throws Exception {
		mockMvc.perform(get("/api/orders/not-a-uuid"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid request parameter"))
			.andExpect(jsonPath("$.status").value(400));
	}

}

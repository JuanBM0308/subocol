package com.subocol.order.api.dto;

import java.util.List;

/**
 * Página de órdenes.
 *
 * @param content órdenes de la página
 * @param page número de página, empezando en cero
 * @param size tamaño de la página
 * @param totalElements total de órdenes que cumplen el filtro
 */
public record OrderPageResponse(List<OrderResponse> content, int page, int size, long totalElements) {

}

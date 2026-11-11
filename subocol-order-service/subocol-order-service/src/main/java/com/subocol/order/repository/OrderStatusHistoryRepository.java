package com.subocol.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subocol.order.domain.OrderStatusHistoryEntity;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, UUID> {

	List<OrderStatusHistoryEntity> findByOrder_IdOrderByChangedAtAsc(UUID orderId);

}

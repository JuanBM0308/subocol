package com.subocol.order.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subocol.order.domain.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

}

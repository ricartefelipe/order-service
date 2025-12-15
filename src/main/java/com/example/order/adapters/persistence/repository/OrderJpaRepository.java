package com.example.order.adapters.persistence.repository;

import com.example.order.adapters.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsById(UUID id);

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsByExternalOrderId(String externalOrderId);

    Optional<OrderEntity> findByExternalOrderId(String externalOrderId);

    Page<OrderEntity> findAllByStatus(String status, Pageable pageable);
}

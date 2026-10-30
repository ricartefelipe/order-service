package com.example.order.adapters.persistence.repository;

import com.example.order.adapters.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsByOriginAndId(String origin, UUID id);

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsByOriginAndExternalOrderId(String origin, String externalOrderId);

    @Query("select distinct o from OrderEntity o left join fetch o.items where o.id in :ids")
    List<OrderEntity> findAllWithItemsByIdIn(@Param("ids") List<UUID> ids);
}

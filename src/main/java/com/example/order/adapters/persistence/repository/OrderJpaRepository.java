package com.example.order.adapters.persistence.repository;

import com.example.order.adapters.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsById(UUID id);

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsByExternalOrderId(String externalOrderId);

    Optional<OrderEntity> findByExternalOrderId(String externalOrderId);

    Page<OrderEntity> findAllByStatus(String status, Pageable pageable);
    @Query("select o.id from OrderEntity o where o.status = :status")
    Page<UUID> findIdsByStatus(@Param("status") String status, Pageable pageable);

    @Query("select o.id from OrderEntity o")
    Page<UUID> findAllIds(Pageable pageable);

    @Query("select distinct o from OrderEntity o left join fetch o.items where o.id in :ids")
    List<OrderEntity> findAllWithItemsByIdIn(@Param("ids") List<UUID> ids);
}

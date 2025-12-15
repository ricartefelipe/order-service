package com.example.order.adapters.persistence.entity;

import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "external_order_id", nullable = false, unique = true, length = 100)
    private String externalOrderId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 100)
    private List<OrderItemEntity> items = new ArrayList<>();

    protected OrderEntity() {
        // for JPA
    }

    public OrderEntity(UUID id, String externalOrderId, String status, BigDecimal totalAmount) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.externalOrderId = Objects.requireNonNull(externalOrderId, "externalOrderId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
    }

    public UUID getId() {
        return id;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }

    public void setItems(List<OrderItemEntity> items) {
        this.items = items;
    }

    public void addItem(OrderItemEntity item) {
        this.items.add(item);
        item.setOrder(this);
    }

    public void clearItems() {
        for (OrderItemEntity item : items) {
            item.setOrder(null);
        }
        items.clear();
    }
}

package com.example.order.adapters.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(
        name = "uk_orders_origin_external_order_id", columnNames = {"origin", "external_order_id"}))
@EntityListeners(AuditingEntityListener.class)
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "origin", nullable = false, length = 100)
    private String origin;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "fingerprint_version", nullable = false, length = 10)
    private String fingerprintVersion;

    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

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

    public OrderEntity(UUID id, String origin, String externalOrderId, String currency, String status,
                       BigDecimal totalAmount, String fingerprintVersion, String fingerprint) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.origin = Objects.requireNonNull(origin, "origin must not be null");
        this.externalOrderId = Objects.requireNonNull(externalOrderId, "externalOrderId must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        this.fingerprintVersion = Objects.requireNonNull(fingerprintVersion, "fingerprintVersion must not be null");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint must not be null");
    }

    public UUID getId() {
        return id;
    }

    public String getOrigin() {
        return origin;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getFingerprintVersion() {
        return fingerprintVersion;
    }

    public String getFingerprint() {
        return fingerprint;
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

    public void addItem(OrderItemEntity item) {
        this.items.add(item);
        item.setOrder(this);
    }
}

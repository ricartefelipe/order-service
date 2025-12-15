package com.example.order.adapters.persistence.mapper;

import com.example.order.adapters.persistence.entity.OrderEntity;
import com.example.order.adapters.persistence.entity.OrderItemEntity;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.model.OrderStatus;
import com.example.order.domain.value.Money;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class OrderPersistenceMapper {

    public OrderEntity toEntity(Order domain) {
        OrderEntity entity = new OrderEntity(
                domain.id(),
                domain.externalOrderId(),
                domain.status().name(),
                domain.totalAmount().toBigDecimal()
        );

        for (OrderItem item : domain.items()) {
            OrderItemEntity itemEntity = new OrderItemEntity(
                    UUID.randomUUID(),
                    item.productId(),
                    item.quantity(),
                    item.unitPrice().toBigDecimal(),
                    item.lineTotal().toBigDecimal()
            );
            entity.addItem(itemEntity);
        }

        return entity;
    }

    public Order toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(this::toDomainItem)
                .toList();

        return new Order(
                entity.getId(),
                entity.getExternalOrderId(),
                OrderStatus.valueOf(entity.getStatus()),
                Money.of(entity.getTotalAmount()),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OrderItem toDomainItem(OrderItemEntity entity) {
        return new OrderItem(
                entity.getProductId(),
                entity.getQuantity(),
                Money.of(entity.getUnitPrice()),
                Money.of(entity.getLineTotal())
        );
    }
}

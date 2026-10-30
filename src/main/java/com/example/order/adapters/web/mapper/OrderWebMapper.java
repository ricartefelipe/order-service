package com.example.order.adapters.web.mapper;

import com.example.order.adapters.web.model.CreateOrderItemRequest;
import com.example.order.adapters.web.model.CreateOrderRequest;
import com.example.order.adapters.web.model.OrderItemResponse;
import com.example.order.adapters.web.model.OrderResponse;
import com.example.order.adapters.web.model.PagedResponse;
import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.CreateOrderItemCommand;
import com.example.order.application.dto.OrderFilter;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.application.pagination.Sort;
import com.example.order.application.pagination.SortDirection;
import com.example.order.application.pagination.SortProperty;
import com.example.order.config.orders.OrdersProperties;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.model.OrderStatus;
import com.example.order.domain.value.Origin;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Component
public class OrderWebMapper {

    private final Currency defaultCurrency;

    public OrderWebMapper(OrdersProperties properties) {
        this.defaultCurrency = properties.defaultCurrency();
    }

    public CreateOrderCommand toCommand(Origin origin, CreateOrderRequest request) {
        List<CreateOrderItemCommand> items = request.items().stream()
                .map(this::toCommandItem)
                .toList();
        return new CreateOrderCommand(origin, request.externalOrderId(), currencyOf(request), items);
    }

    public OrderFilter toFilter(OrderStatus status, String externalOrderId) {
        Optional<String> reference = Optional.ofNullable(externalOrderId).filter(value -> !value.isBlank());
        return new OrderFilter(Optional.ofNullable(status), reference);
    }

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.items().stream()
                .map(this::toResponseItem)
                .toList();
        return new OrderResponse(
                order.id(),
                order.origin().value(),
                order.externalOrderId(),
                order.currency().getCurrencyCode(),
                order.status().name(),
                order.totalAmount().toBigDecimal(),
                items,
                order.createdAt(),
                order.updatedAt()
        );
    }

    public PagedResponse<OrderResponse> toPagedResponse(PageResult<Order> pageResult) {
        List<OrderResponse> content = pageResult.content().stream()
                .map(this::toResponse)
                .toList();
        return new PagedResponse<>(
                content,
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }

    public PageQuery toPageQuery(Pageable pageable) {
        List<Sort> sorts = new ArrayList<>();
        if (pageable.getSort().isSorted()) {
            for (org.springframework.data.domain.Sort.Order springOrder : pageable.getSort()) {
                SortProperty property = SortProperty.fromApiName(springOrder.getProperty())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unsupported sort property: " + springOrder.getProperty()));
                SortDirection direction = springOrder.isDescending() ? SortDirection.DESC : SortDirection.ASC;
                sorts.add(new Sort(property, direction));
            }
        } else {
            sorts.add(new Sort(SortProperty.CREATED_AT, SortDirection.DESC));
        }
        return new PageQuery(pageable.getPageNumber(), pageable.getPageSize(), sorts);
    }

    private Currency currencyOf(CreateOrderRequest request) {
        if (request.currency() == null) {
            return defaultCurrency;
        }
        try {
            return Currency.getInstance(request.currency());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("currency is not a known ISO 4217 code: " + request.currency());
        }
    }

    private CreateOrderItemCommand toCommandItem(CreateOrderItemRequest item) {
        return new CreateOrderItemCommand(item.productId(), item.quantity(), item.unitPrice());
    }

    private OrderItemResponse toResponseItem(OrderItem item) {
        return new OrderItemResponse(
                item.productId(),
                item.quantity(),
                item.unitPrice().toBigDecimal(),
                item.lineTotal().toBigDecimal()
        );
    }
}

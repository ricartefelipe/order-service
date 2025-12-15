package com.example.order.adapters.web.mapper;

import com.example.order.adapters.web.model.CreateOrderItemRequest;
import com.example.order.adapters.web.model.CreateOrderRequest;
import com.example.order.adapters.web.model.OrderItemResponse;
import com.example.order.adapters.web.model.OrderResponse;
import com.example.order.adapters.web.model.PagedResponse;
import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.CreateOrderItemCommand;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.application.pagination.Sort;
import com.example.order.application.pagination.SortDirection;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class OrderWebMapper {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt",
            "externalOrderId",
            "status",
            "totalAmount"
    );

    public CreateOrderCommand toCommand(CreateOrderRequest request) {
        List<CreateOrderItemCommand> items = request.items().stream()
                .map(this::toCommandItem)
                .toList();
        return new CreateOrderCommand(request.externalOrderId(), items);
    }

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.items().stream()
                .map(this::toResponseItem)
                .toList();

        return new OrderResponse(
                order.id(),
                order.externalOrderId(),
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
                String property = springOrder.getProperty();
                if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
                    throw new IllegalArgumentException("Unsupported sort property: " + property);
                }
                SortDirection direction = springOrder.isDescending() ? SortDirection.DESC : SortDirection.ASC;
                sorts.add(new Sort(property, direction));
            }
        } else {
            sorts.add(new Sort("createdAt", SortDirection.DESC));
        }

        return new PageQuery(pageable.getPageNumber(), pageable.getPageSize(), sorts);
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

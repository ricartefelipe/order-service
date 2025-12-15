package com.example.order.adapters.web.controller;

import com.example.order.adapters.web.mapper.OrderWebMapper;
import com.example.order.adapters.web.model.CreateOrderRequest;
import com.example.order.adapters.web.model.OrderResponse;
import com.example.order.adapters.web.model.PagedResponse;
import com.example.order.application.dto.CreateOrderResult;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.application.usecase.CreateOrderUseCase;
import com.example.order.application.usecase.GetOrderUseCase;
import com.example.order.application.usecase.ListOrdersUseCase;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;
    private final OrderWebMapper mapper;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            GetOrderUseCase getOrderUseCase,
            ListOrdersUseCase listOrdersUseCase,
            OrderWebMapper mapper
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.listOrdersUseCase = listOrdersUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResult result = createOrderUseCase.create(mapper.toCommand(request));
        OrderResponse response = mapper.toResponse(result.order());

        if (result.created()) {
            URI location = URI.create("/orders/" + response.id());
            return ResponseEntity.created(location).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable("id") UUID id) {
        Order order = getOrderUseCase.getById(id);
        return ResponseEntity.ok(mapper.toResponse(order));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponse>> list(
            @RequestParam(value = "status", required = false) OrderStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageQuery pageQuery = mapper.toPageQuery(pageable);
        PageResult<Order> result = listOrdersUseCase.list(Optional.ofNullable(status), pageQuery);
        return ResponseEntity.ok(mapper.toPagedResponse(result));
    }
}

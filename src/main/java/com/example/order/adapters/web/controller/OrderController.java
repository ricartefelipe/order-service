package com.example.order.adapters.web.controller;

import com.example.order.adapters.web.mapper.OrderWebMapper;
import com.example.order.adapters.web.model.CreateOrderRequest;
import com.example.order.adapters.web.model.OrderResponse;
import com.example.order.adapters.web.model.PagedResponse;
import com.example.order.adapters.web.security.AuthenticatedOrigin;
import com.example.order.application.dto.CreateOrderResult;
import com.example.order.application.dto.OrderFilter;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.application.usecase.CreateOrderUseCase;
import com.example.order.application.usecase.GetOrderUseCase;
import com.example.order.application.usecase.ListOrdersUseCase;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Ingestão idempotente e consulta de pedidos, sempre no escopo da origem autenticada")
@SecurityRequirement(name = "clientToken")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;
    private final OrderWebMapper mapper;
    private final AuthenticatedOrigin authenticatedOrigin;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            GetOrderUseCase getOrderUseCase,
            ListOrdersUseCase listOrdersUseCase,
            OrderWebMapper mapper,
            AuthenticatedOrigin authenticatedOrigin
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.listOrdersUseCase = listOrdersUseCase;
        this.mapper = mapper;
        this.authenticatedOrigin = authenticatedOrigin;
    }

    @PostMapping
    @Operation(summary = "Cria um pedido (escopo orders:create)",
            description = "201 na criação; 200 ao repetir o mesmo externalOrderId com o mesmo payload; "
                    + "409 ao repetir o externalOrderId com payload diferente.")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResult result = createOrderUseCase.create(mapper.toCommand(authenticatedOrigin.current(), request));
        OrderResponse response = mapper.toResponse(result.order());
        if (result.created()) {
            URI location = URI.create("/orders/" + response.id());
            return ResponseEntity.created(location).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um pedido da origem autenticada (escopo orders:read)")
    public ResponseEntity<OrderResponse> getById(@PathVariable("id") UUID id) {
        Order order = getOrderUseCase.getById(authenticatedOrigin.current(), id);
        return ResponseEntity.ok(mapper.toResponse(order));
    }

    @GetMapping
    @Operation(summary = "Lista pedidos da origem autenticada (escopo orders:read)",
            description = "Filtros opcionais por status e por externalOrderId; ordenação determinística.")
    public ResponseEntity<PagedResponse<OrderResponse>> list(
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "externalOrderId", required = false) String externalOrderId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageQuery pageQuery = mapper.toPageQuery(pageable);
        OrderFilter filter = mapper.toFilter(status, externalOrderId);
        PageResult<Order> result = listOrdersUseCase.list(authenticatedOrigin.current(), filter, pageQuery);
        return ResponseEntity.ok(mapper.toPagedResponse(result));
    }
}

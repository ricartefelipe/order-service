package com.example.order.application.service;

import com.example.order.application.exception.OrderNotFoundException;
import com.example.order.application.port.OrderRepositoryPort;
import com.example.order.application.usecase.GetOrderUseCase;
import com.example.order.domain.model.Order;
import com.example.order.domain.value.Origin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetOrderService implements GetOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public GetOrderService(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getById(Origin origin, UUID id) {
        return orderRepositoryPort.findById(origin, id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}

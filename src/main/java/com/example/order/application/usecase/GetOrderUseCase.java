package com.example.order.application.usecase;

import com.example.order.domain.model.Order;

import java.util.UUID;

public interface GetOrderUseCase {

    Order getById(UUID id);
}

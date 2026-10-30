package com.example.order.application.usecase;

import com.example.order.domain.model.Order;
import com.example.order.domain.value.Origin;

import java.util.UUID;

public interface GetOrderUseCase {

    Order getById(Origin origin, UUID id);
}

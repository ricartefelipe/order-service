package com.example.order.application.usecase;

import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.CreateOrderResult;

public interface CreateOrderUseCase {

    CreateOrderResult create(CreateOrderCommand command);
}

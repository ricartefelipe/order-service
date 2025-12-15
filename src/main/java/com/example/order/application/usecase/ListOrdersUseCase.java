package com.example.order.application.usecase;

import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;

import java.util.Optional;

public interface ListOrdersUseCase {

    PageResult<Order> list(Optional<OrderStatus> status, PageQuery pageQuery);
}

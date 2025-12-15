package com.example.order.application.port;

import com.example.order.application.exception.DuplicateOrderException;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

    Order saveNew(Order order) throws DuplicateOrderException;

    Optional<Order> findByExternalOrderId(String externalOrderId);

    Optional<Order> findById(UUID id);

    PageResult<Order> findAll(Optional<OrderStatus> status, PageQuery pageQuery);
}

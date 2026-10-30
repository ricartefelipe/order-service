package com.example.order.application.port;

import com.example.order.application.dto.OrderFilter;
import com.example.order.application.exception.DuplicateOrderException;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.domain.model.Order;
import com.example.order.domain.value.Origin;

import java.util.Optional;
import java.util.UUID;

/** Persistence port. Every read is scoped by origin: an origin never sees another origin's orders. */
public interface OrderRepositoryPort {

    Order saveNew(Order order) throws DuplicateOrderException;

    Optional<Order> findByExternalOrderId(Origin origin, String externalOrderId);

    Optional<Order> findById(Origin origin, UUID id);

    PageResult<Order> findAll(Origin origin, OrderFilter filter, PageQuery pageQuery);
}

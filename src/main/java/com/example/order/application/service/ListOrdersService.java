package com.example.order.application.service;

import com.example.order.application.dto.OrderFilter;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.application.port.OrderRepositoryPort;
import com.example.order.application.usecase.ListOrdersUseCase;
import com.example.order.domain.model.Order;
import com.example.order.domain.value.Origin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListOrdersService implements ListOrdersUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public ListOrdersService(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Order> list(Origin origin, OrderFilter filter, PageQuery pageQuery) {
        return orderRepositoryPort.findAll(origin, filter, pageQuery);
    }
}

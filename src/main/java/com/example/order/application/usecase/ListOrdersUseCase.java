package com.example.order.application.usecase;

import com.example.order.application.dto.OrderFilter;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.domain.model.Order;
import com.example.order.domain.value.Origin;

public interface ListOrdersUseCase {

    PageResult<Order> list(Origin origin, OrderFilter filter, PageQuery pageQuery);
}

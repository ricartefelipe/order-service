package com.example.order.adapters.persistence.adapter;

import com.example.order.adapters.persistence.entity.OrderEntity;
import com.example.order.adapters.persistence.mapper.OrderPersistenceMapper;
import com.example.order.adapters.persistence.repository.OrderJpaRepository;
import com.example.order.application.dto.OrderFilter;
import com.example.order.application.exception.DuplicateOrderException;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.application.port.OrderRepositoryPort;
import com.example.order.domain.model.Order;
import com.example.order.domain.value.Origin;
import jakarta.persistence.EntityManager;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

    private final OrderJpaRepository repository;
    private final OrderPersistenceMapper mapper;
    private final EntityManager entityManager;

    public OrderRepositoryAdapter(OrderJpaRepository repository, OrderPersistenceMapper mapper, EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public Order saveNew(Order order) throws DuplicateOrderException {
        OrderEntity entity = mapper.toEntity(order);
        try {
            OrderEntity saved = repository.saveAndFlush(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            entityManager.clear();
            if (isUniqueViolation(ex)) {
                throw new DuplicateOrderException(order.externalOrderId(), ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<Order> findByExternalOrderId(Origin origin, String externalOrderId) {
        return repository.findWithItemsByOriginAndExternalOrderId(origin.value(), externalOrderId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findById(Origin origin, UUID id) {
        return repository.findWithItemsByOriginAndId(origin.value(), id)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<Order> findAll(Origin origin, OrderFilter filter, PageQuery pageQuery) {
        OrderPageQuery query = new OrderPageQuery(origin, filter, pageQuery);
        long total = query.count(entityManager);
        List<UUID> ids = total == 0 ? List.of() : query.ids(entityManager);
        int totalPages = (int) Math.ceil((double) total / pageQuery.size());
        if (ids.isEmpty()) {
            return PageResult.of(List.of(), pageQuery.page(), pageQuery.size(), total, totalPages);
        }
        List<OrderEntity> entities = repository.findAllWithItemsByIdIn(ids);
        Map<UUID, Integer> position = new HashMap<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            position.put(ids.get(i), i);
        }
        entities.sort(Comparator.comparingInt(e -> position.getOrDefault(e.getId(), Integer.MAX_VALUE)));
        List<Order> content = entities.stream()
                .map(mapper::toDomain)
                .toList();
        return PageResult.of(content, pageQuery.page(), pageQuery.size(), total, totalPages);
    }

    private static boolean isUniqueViolation(DataIntegrityViolationException ex) {
        Throwable mostSpecific = NestedExceptionUtils.getMostSpecificCause(ex);
        return mostSpecific instanceof SQLException sqlException
                && UNIQUE_VIOLATION_SQLSTATE.equals(sqlException.getSQLState());
    }
}

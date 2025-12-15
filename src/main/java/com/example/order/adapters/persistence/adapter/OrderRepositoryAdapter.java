package com.example.order.adapters.persistence.adapter;

import com.example.order.adapters.persistence.entity.OrderEntity;
import com.example.order.adapters.persistence.mapper.OrderPersistenceMapper;
import com.example.order.adapters.persistence.repository.OrderJpaRepository;
import com.example.order.application.exception.DuplicateOrderException;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.PageResult;
import com.example.order.application.port.OrderRepositoryPort;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;
import jakarta.persistence.EntityManager;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {

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
    public Optional<Order> findByExternalOrderId(String externalOrderId) {
        return repository.findWithItemsByExternalOrderId(externalOrderId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findWithItemsById(id)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<Order> findAll(Optional<OrderStatus> status, PageQuery pageQuery) {
        Pageable pageable = toPageable(pageQuery);

        Page<UUID> idPage = status
                .map(s -> repository.findIdsByStatus(s.name(), pageable))
                .orElseGet(() -> repository.findAllIds(pageable));

        List<Order> content = fetchInOrder(idPage.getContent()).stream()
                .map(mapper::toDomain)
                .toList();

        return PageResult.of(
                content,
                idPage.getNumber(),
                idPage.getSize(),
                idPage.getTotalElements(),
                idPage.getTotalPages()
        );
    }

    private List<OrderEntity> fetchInOrder(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<OrderEntity> loaded = repository.findAllWithItemsByIdIn(ids);
        Map<UUID, OrderEntity> byId = loaded.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(OrderEntity::getId, Function.identity(), (left, right) -> left));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private static Pageable toPageable(PageQuery pageQuery) {
        Sort sort = Sort.unsorted();
        for (com.example.order.application.pagination.Sort s : pageQuery.sorts()) {
            Sort springSort = Sort.by(s.property());
            springSort = (s.direction() == com.example.order.application.pagination.SortDirection.DESC)
                    ? springSort.descending()
                    : springSort.ascending();
            sort = sort.and(springSort);
        }
        return PageRequest.of(pageQuery.page(), pageQuery.size(), sort);
    }

    private static boolean isUniqueViolation(DataIntegrityViolationException ex) {
        Throwable mostSpecific = NestedExceptionUtils.getMostSpecificCause(ex);
        if (mostSpecific instanceof SQLException sqlException) {
            return "23505".equals(sqlException.getSQLState());
        }
        return false;
    }
}

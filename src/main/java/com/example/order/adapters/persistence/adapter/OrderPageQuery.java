package com.example.order.adapters.persistence.adapter;

import com.example.order.application.dto.OrderFilter;
import com.example.order.application.pagination.PageQuery;
import com.example.order.application.pagination.Sort;
import com.example.order.application.pagination.SortDirection;
import com.example.order.domain.value.Origin;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the id page of a listing: one count and one id query, both restricted to the origin and to
 * the optional filter. The order is always made deterministic with {@code id} as the last key.
 * Entity attribute names equal the API sort names, so {@link Sort#property()} maps directly.
 */
final class OrderPageQuery {

    private static final String TIE_BREAKER = "o.id asc";

    private final Map<String, Object> parameters;
    private final PageQuery pageQuery;

    OrderPageQuery(Origin origin, OrderFilter filter, PageQuery pageQuery) {
        this.parameters = parameters(origin, filter);
        this.pageQuery = pageQuery;
    }

    long count(EntityManager entityManager) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(o) from OrderEntity o " + whereClause(), Long.class);
        bind(query);
        return query.getSingleResult();
    }

    List<UUID> ids(EntityManager entityManager) {
        TypedQuery<UUID> query = entityManager.createQuery(
                "select o.id from OrderEntity o " + whereClause() + " order by " + orderClause(), UUID.class);
        bind(query);
        query.setFirstResult(pageQuery.page() * pageQuery.size());
        query.setMaxResults(pageQuery.size());
        return query.getResultList();
    }

    private String whereClause() {
        List<String> predicates = new ArrayList<>();
        for (String name : parameters.keySet()) {
            predicates.add("o." + name + " = :" + name);
        }
        return "where " + String.join(" and ", predicates);
    }

    private String orderClause() {
        List<String> keys = new ArrayList<>();
        for (Sort sort : pageQuery.sorts()) {
            String direction = sort.direction() == SortDirection.DESC ? "desc" : "asc";
            keys.add("o." + sort.property().apiName() + " " + direction);
        }
        keys.add(TIE_BREAKER);
        return String.join(", ", keys);
    }

    private static Map<String, Object> parameters(Origin origin, OrderFilter filter) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("origin", origin.value());
        filter.status().ifPresent(status -> parameters.put("status", status.name()));
        filter.externalOrderId().ifPresent(externalOrderId -> parameters.put("externalOrderId", externalOrderId));
        return parameters;
    }

    private void bind(TypedQuery<?> query) {
        parameters.forEach(query::setParameter);
    }
}

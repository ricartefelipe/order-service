package com.example.order.application.pagination;

import java.util.Arrays;
import java.util.Optional;

/** Properties a listing can be sorted by. The API name is the one accepted in the {@code sort} parameter. */
public enum SortProperty {
    CREATED_AT("createdAt"),
    EXTERNAL_ORDER_ID("externalOrderId"),
    STATUS("status"),
    TOTAL_AMOUNT("totalAmount");

    private final String apiName;

    SortProperty(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }

    public static Optional<SortProperty> fromApiName(String apiName) {
        return Arrays.stream(values())
                .filter(property -> property.apiName.equals(apiName))
                .findFirst();
    }
}

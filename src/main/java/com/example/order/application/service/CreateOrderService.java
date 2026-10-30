package com.example.order.application.service;

import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.CreateOrderItemCommand;
import com.example.order.application.dto.CreateOrderResult;
import com.example.order.application.exception.DuplicateOrderException;
import com.example.order.application.exception.IdempotencyConflictException;
import com.example.order.application.port.OrderIngestionMetrics;
import com.example.order.application.port.OrderRepositoryPort;
import com.example.order.application.usecase.CreateOrderUseCase;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.value.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Idempotent ingestion. The insert runs first and relies on the unique index
 * {@code (origin, external_order_id)}; on a unique violation the stored order is read in a separate
 * read-only transaction and compared by fingerprint: same payload replays the original, a different
 * payload conflicts and nothing is changed.
 */
@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderIngestionMetrics metrics;
    private final TransactionTemplate writeTx;
    private final TransactionTemplate readTx;

    public CreateOrderService(OrderRepositoryPort orderRepositoryPort,
                              OrderIngestionMetrics metrics,
                              PlatformTransactionManager transactionManager) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.metrics = metrics;
        TransactionTemplate write = new TransactionTemplate(transactionManager);
        write.setReadOnly(false);
        this.writeTx = write;
        TransactionTemplate read = new TransactionTemplate(transactionManager);
        read.setReadOnly(true);
        this.readTx = read;
    }

    @Override
    public CreateOrderResult create(CreateOrderCommand command) {
        Order candidate = buildOrder(command);
        try {
            Order saved = writeTx.execute(status -> orderRepositoryPort.saveNew(candidate));
            if (saved == null) {
                throw new IllegalStateException("Transaction returned null (saveNew)");
            }
            metrics.created();
            return new CreateOrderResult(saved, true);
        } catch (DuplicateOrderException ex) {
            return replayOrConflict(candidate);
        }
    }

    private CreateOrderResult replayOrConflict(Order candidate) {
        Order existing = readTx.execute(status ->
                orderRepositoryPort.findByExternalOrderId(candidate.origin(), candidate.externalOrderId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Unique constraint violated but order not found by externalOrderId="
                                        + candidate.externalOrderId())));
        if (existing == null) {
            throw new IllegalStateException("Transaction returned null (findByExternalOrderId)");
        }
        if (!existing.hasSamePayloadAs(candidate)) {
            metrics.conflicted();
            throw new IdempotencyConflictException(candidate.externalOrderId());
        }
        metrics.replayed();
        return new CreateOrderResult(existing, false);
    }

    private static Order buildOrder(CreateOrderCommand command) {
        List<OrderItem> items = command.items().stream()
                .map(CreateOrderService::toDomainItem)
                .toList();
        return Order.newReceived(command.origin(), command.externalOrderId(), command.currency(), items)
                .markCalculated();
    }

    private static OrderItem toDomainItem(CreateOrderItemCommand item) {
        Money unitPrice = Money.of(item.unitPrice());
        return OrderItem.of(item.productId(), item.quantity(), unitPrice);
    }
}

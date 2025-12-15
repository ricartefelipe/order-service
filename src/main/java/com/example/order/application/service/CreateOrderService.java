package com.example.order.application.service;

import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.CreateOrderItemCommand;
import com.example.order.application.dto.CreateOrderResult;
import com.example.order.application.exception.DuplicateOrderException;
import com.example.order.application.port.OrderRepositoryPort;
import com.example.order.application.usecase.CreateOrderUseCase;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.value.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final TransactionTemplate writeTx;
    private final TransactionTemplate readTx;

    public CreateOrderService(OrderRepositoryPort orderRepositoryPort, PlatformTransactionManager transactionManager) {
        this.orderRepositoryPort = orderRepositoryPort;

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
            return new CreateOrderResult(saved, true);
        } catch (DuplicateOrderException ex) {
            Order existing = readTx.execute(status -> orderRepositoryPort.findByExternalOrderId(command.externalOrderId())
                    .orElseThrow(() -> new IllegalStateException("Unique constraint violated but order not found by externalOrderId=" + command.externalOrderId())));
            if (existing == null) {
                throw new IllegalStateException("Transaction returned null (findByExternalOrderId)");
            }
            return new CreateOrderResult(existing, false);
        }
    }

    private static Order buildOrder(CreateOrderCommand command) {
        List<OrderItem> items = command.items().stream()
                .map(CreateOrderService::toDomainItem)
                .toList();

        // Demonstrate domain lifecycle: RECEIVED -> CALCULATED (final persisted status is CALCULATED)
        return Order.newReceived(command.externalOrderId(), items).markCalculated();
    }

    private static OrderItem toDomainItem(CreateOrderItemCommand item) {
        Money unitPrice = Money.of(item.unitPrice());
        return OrderItem.of(item.productId(), item.quantity(), unitPrice);
    }
}

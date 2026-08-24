package com.bankapp.payments.infrastructure.events;

import com.bankapp.payments.application.port.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Context-qualified name on purpose: Spring derives a bean name from the simple
 * class name, so a second {@code SpringDomainEventPublisher} in another package
 * collides with the accounts one at startup.
 *
 * <p>Events are held until the transaction commits. A domain event is a statement
 * that something <em>happened</em>, and nothing has happened until the transaction
 * that did it is durable — a handler can still throw, and the unique index on
 * {@code idempotency_key} rejects a concurrent duplicate at flush, well after the
 * publish call. Delivering on the spot would announce transfers that then rolled
 * back, which is how one retry becomes several payments downstream (ADR-003 §5).
 */
@Component
class SpringPaymentsDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    SpringPaymentsDomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No transaction in progress — nothing to wait for.
            delegate.publishEvent(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delegate.publishEvent(event);
                }
            }
        );
    }
}

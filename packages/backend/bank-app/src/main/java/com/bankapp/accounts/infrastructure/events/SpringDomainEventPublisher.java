package com.bankapp.accounts.infrastructure.events;

import com.bankapp.accounts.application.port.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Events are held until the transaction commits. A domain event says something
 * <em>happened</em>, and nothing has happened until the transaction that did it is
 * durable — a handler can still throw, and constraint violations only surface at
 * flush, after the publish call. Delivering on the spot would announce state that
 * then rolled back.
 *
 * <p>This makes "published once per committed change" true at the port, so
 * consumers get it whether or not they remember
 * {@code @TransactionalEventListener(AFTER_COMMIT)}. It is also the behaviour a
 * Kafka adapter needs (docs/09): writing to a broker before the database commits
 * is the dual-write problem.
 */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    SpringDomainEventPublisher(ApplicationEventPublisher delegate) {
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

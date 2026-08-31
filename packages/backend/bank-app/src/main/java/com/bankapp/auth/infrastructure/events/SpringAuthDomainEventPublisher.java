package com.bankapp.auth.infrastructure.events;

import com.bankapp.auth.application.port.DomainEventPublisher;
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
 * {@code users.email} rejects a concurrent duplicate registration at flush, well
 * after the publish call. Delivering on the spot would announce a
 * {@code UserRegistered} for a row that never existed, and every downstream
 * consumer — welcome email, fraud profile — would act on a user who cannot log in.
 */
@Component
class SpringAuthDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    SpringAuthDomainEventPublisher(ApplicationEventPublisher delegate) {
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

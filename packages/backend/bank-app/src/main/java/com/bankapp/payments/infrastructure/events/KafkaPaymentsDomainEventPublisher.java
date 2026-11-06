package com.bankapp.payments.infrastructure.events;

import com.bankapp.payments.application.port.DomainEventPublisher;
import com.bankapp.payments.domain.events.PaymentCompleted;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Context-qualified name on purpose: Spring derives a bean name from the simple
 * class name, so a second {@code KafkaDomainEventPublisher} in another package
 * collides with the fraud-detection one at startup.
 *
 * <p>Events are held until the transaction commits, then sent to Kafka. A domain
 * event is a statement that something <em>happened</em>, and nothing has happened
 * until the transaction that did it is durable — the unique index on
 * {@code idempotency_key} rejects a concurrent duplicate at flush, well after the
 * publish call. Sending to the broker before the database commits is the
 * dual-write problem (docs/09): the broker would announce transfers that then
 * rolled back, which is how one retry becomes several payments downstream
 * (ADR-003 §5).
 */
@Component
class KafkaPaymentsDomainEventPublisher implements DomainEventPublisher {

    static final String PAYMENT_COMPLETED_TOPIC = "payment-completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    KafkaPaymentsDomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No transaction in progress — nothing to wait for.
            send(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            }
        );
    }

    private void send(Object event) {
        if (event instanceof PaymentCompleted completed) {
            kafkaTemplate.send(
                PAYMENT_COMPLETED_TOPIC,
                completed.transferId().toString(),
                completed
            );
        }
    }
}

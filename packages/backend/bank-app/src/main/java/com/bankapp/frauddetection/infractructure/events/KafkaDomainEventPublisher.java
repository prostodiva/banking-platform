package com.bankapp.frauddetection.infrastructure.events;

import com.bankapp.frauddetection.application.port.DomainEventPublisher;
import com.bankapp.frauddetection.domain.events.FraudAlertRaised;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaDomainEventPublisher implements DomainEventPublisher {

    static final String FRAUD_ALERT_RAISED_TOPIC = "fraud-alert-raised";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    KafkaDomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(Object event) {
        if (event instanceof FraudAlertRaised raised) {
            kafkaTemplate.send(
                FRAUD_ALERT_RAISED_TOPIC,
                raised.transferId().toString(),
                raised
            );
        }
    }
}

package com.bankapp.payments.application.port;

public interface DomainEventPublisher {
    void publish(Object event);
}

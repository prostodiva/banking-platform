package com.bankapp.accounts.application.port;

public interface DomainEventPublisher {
    void publish(Object event);
}

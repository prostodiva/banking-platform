package com.bankapp.auth.application.port;

public interface DomainEventPublisher {
    void publish(Object event);
}

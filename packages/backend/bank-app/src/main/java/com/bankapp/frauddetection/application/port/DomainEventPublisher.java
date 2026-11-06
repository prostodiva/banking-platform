package com.bankapp.frauddetection.application.port;

public interface DomainEventPublisher {
    void publish(Object event);
}

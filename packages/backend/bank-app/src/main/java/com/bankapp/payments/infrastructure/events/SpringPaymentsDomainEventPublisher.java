package com.bankapp.payments.infrastructure.events;

import com.bankapp.payments.application.port.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Context-qualified name on purpose: Spring derives a bean name from the simple
 * class name, so a second {@code SpringDomainEventPublisher} in another package
 * collides with the accounts one at startup.
 */
@Component
class SpringPaymentsDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    SpringPaymentsDomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(Object event) {
        delegate.publishEvent(event);
    }
}

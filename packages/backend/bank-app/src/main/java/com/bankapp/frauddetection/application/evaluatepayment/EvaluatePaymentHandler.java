package com.bankapp.frauddetection.application.evaluatepayment;

import com.bankapp.frauddetection.application.port.DomainEventPublisher;
import com.bankapp.frauddetection.domain.FraudAlert;
import com.bankapp.frauddetection.domain.FraudAlertRepository;
import com.bankapp.frauddetection.domain.events.FraudAlertRaised;
import com.bankapp.shared.domain.Money;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluatePaymentHandler {

    private final FraudAlertRepository alerts;
    private final DomainEventPublisher events;
    private final BigDecimal largePaymentThreshold;

    public EvaluatePaymentHandler(
        FraudAlertRepository alerts,
        DomainEventPublisher events,
        @Value("${fraud.large-payment-threshold}") BigDecimal largePaymentThreshold
    ) {
        this.alerts = alerts;
        this.events = events;
        this.largePaymentThreshold = largePaymentThreshold;
    }

    @Transactional
    public void handle(EvaluatePaymentCommand command) {
        if (alerts.existsByTransferId(command.transferId())) {
            return;
        }
        if (command.amount().compareTo(largePaymentThreshold) <= 0) {
            return;
        }

        FraudAlert alert = FraudAlert.raise(
            command.transferId(),
            command.fromAccountId(),
            new Money(command.amount(), command.currencyCode())
        );
        alerts.save(alert);
        events.publish(
            new FraudAlertRaised(
                alert.getId(),
                alert.getTransferId(),
                alert.getFromAccountId(),
                command.amount(),
                command.currencyCode(),
                alert.getCreatedAt()
            )
        );
    }
}

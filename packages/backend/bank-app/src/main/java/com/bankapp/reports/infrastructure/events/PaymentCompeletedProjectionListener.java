package com.bankapp.reports.infrastructure.events;

import com.bankapp.reports.application.projectpayment.ProjectPaymentCommand;
import com.bankapp.reports.application.projectpayment.ProjectPaymentHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class PaymentCompletedProjectionListener {

    private final ProjectPaymentHandler handler;

    PaymentCompletedProjectionListener(ProjectPaymentHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(
        topics = "payment-completed",
        groupId = "reports",
        properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=com.bankapp.reports.infrastructure.events.PaymentCompletedMessage"
        }
    )
    void onPaymentCompleted(PaymentCompletedMessage message) {
        handler.handle(
            new ProjectPaymentCommand(
                message.transferId(),
                message.fromAccountId(),
                message.toAccountId(),
                message.amount(),
                message.currencyCode(),
                message.occurredAt()
            )
        );
    }
}

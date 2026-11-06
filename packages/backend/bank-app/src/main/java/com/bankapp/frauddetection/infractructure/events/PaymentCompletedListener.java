package com.bankapp.frauddetection.infrastructure.events;

import com.bankapp.frauddetection.application.evaluatepayment.EvaluatePaymentCommand;
import com.bankapp.frauddetection.application.evaluatepayment.EvaluatePaymentHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class PaymentCompletedListener {

    private final EvaluatePaymentHandler handler;

    PaymentCompletedListener(EvaluatePaymentHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(
        topics = "payment-completed",
        groupId = "fraud-detection",
        properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=com.bankapp.frauddetection.infrastructure.events.PaymentCompletedMessage"
        }
    )

    void onPaymentCompleted(PaymentCompletedMessage message) {
        handler.handle(
            new EvaluatePaymentCommand(
                message.transferId(),
                message.fromAccountId(),
                message.amount(),
                message.currencyCode()
            )
        );
    }
}

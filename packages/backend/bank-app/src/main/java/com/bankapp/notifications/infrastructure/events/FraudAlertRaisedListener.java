package com.bankapp.notifications.infrastructure.events;

import com.bankapp.notifications.application.notifyonfraudalert.NotifyOnFraudAlertCommand;
import com.bankapp.notifications.application.notifyonfraudalert.NotifyOnFraudAlertHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class FraudAlertRaisedListener {

    private static final Logger log = LoggerFactory.getLogger(FraudAlertRaisedListener.class);

    private final NotifyOnFraudAlertHandler handler;

    FraudAlertRaisedListener(NotifyOnFraudAlertHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(
            topics = "fraud-alert-raised",
            groupId = "notifications",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.bankapp.notifications.infrastructure.events.FraudAlertRaisedMessage"
            })
    void onFraudAlertRaised(FraudAlertRaisedMessage message) {
        if (message.alertId() == null || message.fromAccountId() == null
                || message.amount() == null || message.currencyCode() == null) {
            log.warn("Skipping malformed FraudAlertRaised event: {}", message);
            return;
        }
        handler.handle(new NotifyOnFraudAlertCommand(
                message.alertId(),
                message.fromAccountId(),
                message.amount(),
                message.currencyCode()));
    }
}

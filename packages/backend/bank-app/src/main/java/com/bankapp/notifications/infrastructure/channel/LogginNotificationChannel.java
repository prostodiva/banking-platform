package com.bankapp.notifications.infrastructure.channel;

import com.bankapp.notifications.application.port.NotificationChannel;
import com.bankapp.notifications.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LoggingNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationChannel.class);

    @Override
    public void send(Notification notification) {
        log.info("Notification {} delivered to user {}: FRAUD_ALERT on account {}, amount {} {} (alert {})",
                notification.getId(),
                notification.getRecipientId(),
                notification.getAccountId(),
                notification.getAmount().amount(),
                notification.getAmount().currencyCode(),
                notification.getAlertId());
    }
}

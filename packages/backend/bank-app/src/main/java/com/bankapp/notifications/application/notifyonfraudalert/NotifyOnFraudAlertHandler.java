package com.bankapp.notifications.application.notifyonfraudalert;

import com.bankapp.accounts.application.port.AccountOwnershipQuery;
import com.bankapp.notifications.application.port.NotificationChannel;
import com.bankapp.notifications.domain.Notification;
import com.bankapp.notifications.domain.NotificationRepository;
import com.bankapp.shared.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class NotifyOnFraudAlertHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyOnFraudAlertHandler.class);

    private final NotificationRepository notifications;
    private final AccountOwnershipQuery ownership;
    private final NotificationChannel channel;

    public NotifyOnFraudAlertHandler(NotificationRepository notifications,
                                     AccountOwnershipQuery ownership,
                                     NotificationChannel channel) {
        this.notifications = notifications;
        this.ownership = ownership;
        this.channel = channel;
    }

    @Transactional
    public void handle(NotifyOnFraudAlertCommand command) {
        if (notifications.existsByAlertId(command.alertId())) {
            return;
        }

        Optional<UUID> recipientId = ownership.ownerOf(command.fromAccountId());
        if (recipientId.isEmpty()) {
            log.warn("Skipping notification for alert {}: owner of account {} not found",
                    command.alertId(), command.fromAccountId());
            return;
        }

        Notification notification = Notification.forFraudAlert(
                command.alertId(),
                recipientId.get(),
                command.fromAccountId(),
                new Money(command.amount(), command.currencyCode()));

        try {
            channel.send(notification);
            notification.markSent();
        } catch (RuntimeException e) {
            log.warn("Notification delivery failed for alert {}: {}",
                    command.alertId(), e.getMessage());
            notification.markFailed();
        }

        notifications.save(notification);
    }
}

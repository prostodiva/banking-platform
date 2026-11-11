package com.bankapp.notifications.application.getnotifications;

import com.bankapp.notifications.domain.Notification;
import com.bankapp.notifications.domain.NotificationStatus;
import com.bankapp.notifications.domain.NotificationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NotificationView(
        UUID id,
        UUID alertId,
        UUID accountId,
        NotificationType type,
        BigDecimal amount,
        String currencyCode,
        NotificationStatus status,
        Instant createdAt
) {

    public static NotificationView from(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getAlertId(),
                notification.getAccountId(),
                notification.getType(),
                notification.getAmount().amount(),
                notification.getAmount().currencyCode(),
                notification.getStatus(),
                notification.getCreatedAt());
    }
}

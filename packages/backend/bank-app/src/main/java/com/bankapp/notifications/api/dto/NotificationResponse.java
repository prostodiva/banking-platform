package com.bankapp.notifications.api.dto;

import com.bankapp.notifications.application.getnotifications.NotificationView;
import com.bankapp.notifications.domain.NotificationStatus;
import com.bankapp.notifications.domain.NotificationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID alertId,
        UUID accountId,
        NotificationType type,
        BigDecimal amount,
        String currencyCode,
        NotificationStatus status,
        Instant createdAt
) {

    public static NotificationResponse from(NotificationView view) {
        return new NotificationResponse(
                view.id(),
                view.alertId(),
                view.accountId(),
                view.type(),
                view.amount(),
                view.currencyCode(),
                view.status(),
                view.createdAt());
    }
}

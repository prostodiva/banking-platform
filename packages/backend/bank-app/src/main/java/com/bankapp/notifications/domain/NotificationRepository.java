package com.bankapp.notifications.domain;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    boolean existsByAlertId(UUID alertId);

    List<Notification> findByRecipientId(UUID recipientId);
}

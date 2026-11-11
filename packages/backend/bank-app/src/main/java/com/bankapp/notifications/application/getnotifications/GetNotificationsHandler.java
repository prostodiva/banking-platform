package com.bankapp.notifications.application.getnotifications;

import com.bankapp.notifications.domain.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetNotificationsHandler {

    private final NotificationRepository notifications;

    public GetNotificationsHandler(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<NotificationView> handle(UUID recipientId) {
        return notifications.findByRecipientId(recipientId).stream()
                .map(NotificationView::from)
                .toList();
    }
}

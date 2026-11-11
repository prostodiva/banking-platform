package com.bankapp.notifications.infrastructure.persistence;

import com.bankapp.notifications.domain.Notification;
import com.bankapp.notifications.domain.NotificationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public boolean existsByAlertId(UUID alertId) {
        return jpaRepository.existsByAlertId(alertId);
    }

    @Override
    public List<Notification> findByRecipientId(UUID recipientId) {
        return jpaRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }
}

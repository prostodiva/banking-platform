package com.bankapp.notifications.infrastructure.persistence;

import com.bankapp.notifications.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface NotificationJpaRepository extends JpaRepository<Notification, UUID> {

    boolean existsByAlertId(UUID alertId);

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
}

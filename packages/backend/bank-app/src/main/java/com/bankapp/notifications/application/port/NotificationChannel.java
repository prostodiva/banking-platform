package com.bankapp.notifications.application.port;

import com.bankapp.notifications.domain.Notification;

/**
 * Delivery strategy port. Implementations deliver one notification through
 * one channel and throw on failure — the handler owns what a failure means
 * (mark FAILED, log, never propagate).
 */
public interface NotificationChannel {

    void send(Notification notification);
}

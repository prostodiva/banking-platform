package com.bankapp.notifications.api;

import com.bankapp.notifications.api.dto.NotificationResponse;
import com.bankapp.notifications.application.getnotifications.GetNotificationsHandler;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final GetNotificationsHandler handler;

    public NotificationController(GetNotificationsHandler handler) {
        this.handler = handler;
    }

    @GetMapping
    public List<NotificationResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        UUID recipientId = UUID.fromString(jwt.getSubject());
        return handler.handle(recipientId).stream()
                .map(NotificationResponse::from)
                .toList();
    }
}

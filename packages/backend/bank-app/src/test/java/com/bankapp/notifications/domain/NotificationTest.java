package com.bankapp.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankapp.shared.domain.Money;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final UUID RECIPIENT_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final Money AMOUNT = new Money(new BigDecimal("15000.00"), "USD");

    @Test
    void forFraudAlertCreatesNotification() {
        Notification notification = Notification.forFraudAlert(
            ALERT_ID, RECIPIENT_ID, ACCOUNT_ID, AMOUNT
        );

        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getAlertId()).isEqualTo(ALERT_ID);
        assertThat(notification.getRecipientId()).isEqualTo(RECIPIENT_ID);
        assertThat(notification.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(notification.getType()).isEqualTo(NotificationType.FRAUD_ALERT);
        assertThat(notification.getAmount().amount()).isEqualByComparingTo(
            new BigDecimal("15000.00")
        );
        assertThat(notification.getAmount().currencyCode()).isEqualTo("USD");
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void forFraudAlertRequiresAnAlertId() {
        assertThatThrownBy(() ->
            Notification.forFraudAlert(null, RECIPIENT_ID, ACCOUNT_ID, AMOUNT)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void forFraudAlertRequiresARecipient() {
        assertThatThrownBy(() ->
            Notification.forFraudAlert(ALERT_ID, null, ACCOUNT_ID, AMOUNT)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void forFraudAlertRequiresAnAccount() {
        assertThatThrownBy(() ->
            Notification.forFraudAlert(ALERT_ID, RECIPIENT_ID, null, AMOUNT)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void forFraudAlertRequiresAnAmount() {
        assertThatThrownBy(() ->
            Notification.forFraudAlert(ALERT_ID, RECIPIENT_ID, ACCOUNT_ID, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markSentRecordsTheSentOutcome() {
        Notification notification = Notification.forFraudAlert(
            ALERT_ID, RECIPIENT_ID, ACCOUNT_ID, AMOUNT
        );

        notification.markSent();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void markFailedRecordsTheFailedOutcome() {
        Notification notification = Notification.forFraudAlert(
            ALERT_ID, RECIPIENT_ID, ACCOUNT_ID, AMOUNT
        );

        notification.markFailed();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void deliveryOutcomeCannotBeRecordedTwice() {
        Notification notification = Notification.forFraudAlert(
            ALERT_ID, RECIPIENT_ID, ACCOUNT_ID, AMOUNT
        );
        notification.markSent();

        assertThatThrownBy(notification::markFailed).isInstanceOf(
            IllegalStateException.class
        );
    }
}

package com.bankapp.accounts.infrastructure.events;

import com.bankapp.accounts.domain.events.MoneyDeposited;
import com.bankapp.accounts.domain.events.MoneyWithdrawn;
import com.bankapp.reports.application.port.AccountMovementProjector;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class MoneyMovementProjectionListener {

    private final AccountMovementProjector projector;

    MoneyMovementProjectionListener(AccountMovementProjector projector) {
        this.projector = projector;
    }

    @EventListener
    public void on(MoneyDeposited event) {
        projector.projectDeposit(
            event.eventId(),
            event.accountId(),
            event.amount(),
            event.currencyCode(),
            event.occurredAt()
        );
    }

    @EventListener
    public void on(MoneyWithdrawn event) {
        projector.projectWithdrawal(
            event.eventId(),
            event.accountId(),
            event.amount(),
            event.currencyCode(),
            event.occurredAt()
        );
    }
}

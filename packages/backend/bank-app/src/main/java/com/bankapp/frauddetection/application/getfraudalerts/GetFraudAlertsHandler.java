package com.bankapp.frauddetection.application.getfraudalerts;

import com.bankapp.frauddetection.domain.FraudAlertRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetFraudAlertsHandler {

    private final FraudAlertRepository alerts;

    public GetFraudAlertsHandler(FraudAlertRepository alerts) {
        this.alerts = alerts;
    }

    @Transactional(readOnly = true)
    public List<FraudAlertView> handle(UUID fromAccountId) {
        return alerts.findByFromAccountId(fromAccountId)
            .stream()
            .map(FraudAlertView::from)
            .toList();
    }
}

package com.bankapp.frauddetection.api;

import com.bankapp.frauddetection.api.dto.FraudAlertResponse;
import com.bankapp.frauddetection.application.getfraudalerts.GetFraudAlertsHandler;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fraud-alerts")
public class FraudAlertController {

    private final GetFraudAlertsHandler getFraudAlerts;

    public FraudAlertController(GetFraudAlertsHandler getFraudAlerts) {
        this.getFraudAlerts = getFraudAlerts;
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<FraudAlertResponse>> listByAccount(
        @PathVariable UUID accountId
    ) {
        List<FraudAlertResponse> response = getFraudAlerts
            .handle(accountId)
            .stream()
            .map(FraudAlertResponse::from)
            .toList();

        return ResponseEntity.ok(response);
    }

}

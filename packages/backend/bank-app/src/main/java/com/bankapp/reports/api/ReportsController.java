package com.bankapp.reports.api;

import com.bankapp.reports.api.dto.StatementResponse;
import com.bankapp.reports.api.dto.TransactionEntryResponse;
import com.bankapp.reports.application.gethistory.GetTransactionHistoryHandler;
import com.bankapp.reports.application.gethistory.GetTransactionHistoryQuery;
import com.bankapp.reports.application.getstatement.GetMonthlyStatementHandler;
import com.bankapp.reports.application.getstatement.GetMonthlyStatementQuery;
import com.bankapp.reports.application.getstatement.StatementView;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private final GetMonthlyStatementHandler getStatement;
    private final GetTransactionHistoryHandler getHistory;

    public ReportsController(
        GetMonthlyStatementHandler getStatement,
        GetTransactionHistoryHandler getHistory
    ) {
        this.getStatement = getStatement;
        this.getHistory = getHistory;
    }

    @GetMapping("/statements")
    public ResponseEntity<StatementResponse> statement(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam UUID accountId,
        @RequestParam YearMonth month
    ) {
        StatementView view = getStatement.handle(
            new GetMonthlyStatementQuery(
                accountId,
                UUID.fromString(jwt.getSubject()),
                month
            )
        );
        return ResponseEntity.ok(StatementResponse.from(view));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionEntryResponse>> transactions(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam UUID accountId,
        @RequestParam LocalDate from,
        @RequestParam LocalDate to
    ) {
        List<TransactionEntryResponse> body = getHistory
            .handle(new GetTransactionHistoryQuery(
                accountId,
                UUID.fromString(jwt.getSubject()),
                from,
                to
            ))
            .stream()
            .map(TransactionEntryResponse::from)
            .toList();
        return ResponseEntity.ok(body);
    }
}

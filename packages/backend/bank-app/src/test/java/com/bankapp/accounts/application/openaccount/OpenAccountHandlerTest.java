package com.bankapp.accounts.application.openaccount;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.AccountStatus;
import com.bankapp.accounts.domain.AccountType;
import com.bankapp.accounts.domain.event.AccountOpened;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpenAccountHandlerTest {

    private static final UUID OWNER = UUID.randomUUID();

    private final Map<UUID, Account> saved = new HashMap<>();
    private final List<Object> published = new ArrayList<>();

    private final AccountRepository repository = new AccountRepository() {
        @Override
        public Account save(Account account) {
            saved.put(account.getId(), account);
            return account;
        }

        @Override
        public Optional<Account> findById(UUID id) {
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public boolean existsByAccountNumber(String accountNumber) {
            return saved
                .values()
                .stream()
                .anyMatch(a -> a.getAccountNumber().equals(accountNumber));
        }
    };

    private final OpenAccountHandler handler = new OpenAccountHandler(
        repository,
        published::add
    );

    @Test
    void opensAndPersistsAnActiveAccount() {
        OpenAccountResult result = handler.handle(
            new OpenAccountCommand(OWNER, AccountType.CHECKING, "USD")
        );

        assertThat(saved).containsKey(result.id());
        assertThat(result.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void publishesAccountOpened() {
        handler.handle(
            new OpenAccountCommand(OWNER, AccountType.CHECKING, "USD")
        );

        assertThat(published)
            .singleElement()
            .isInstanceOfSatisfying(AccountOpened.class, event ->
                assertThat(event.ownerId()).isEqualTo(OWNER)
            );
    }
}

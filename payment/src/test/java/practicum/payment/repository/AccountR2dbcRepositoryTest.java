package practicum.payment.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import practicum.payment.entity.Account;
import practicum.payment.r2dbc.AccountR2dbcRepository;
import practicum.payment.util.BaseContextTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

class AccountR2dbcRepositoryTest extends BaseContextTest {

    @Autowired
    private AccountR2dbcRepository accountRepository;

    @BeforeEach
    void setUp() {
        dbHelper.clearAndResetAccounts()
                .then(dbHelper.createMockAccount(1L, new BigDecimal("50.00")))
                .block();
    }

    @Test
    void findByUserId_shouldReturnAccount() {
        Mono<Account> result = accountRepository.findByUserId(1L);

        StepVerifier.create(result)
                .expectNextMatches(account ->
                        account.getUserId().equals(1L)
                                && account.getBalance().compareTo(new BigDecimal("50.00")) == 0
                )
                .verifyComplete();
    }

    @Test
    void findByUserId_shouldReturnEmpty_whenNotFound() {
        Mono<Account> result = accountRepository.findByUserId(99L);

        StepVerifier.create(result)
                .verifyComplete();
    }
}

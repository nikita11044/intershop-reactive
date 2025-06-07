package practicum.payment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import practicum.payment.entity.Account;
import practicum.payment.r2dbc.AccountR2dbcRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountR2dbcRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void getBalance_shouldReturnBalance_whenAccountExists() {
        Long userId = 1L;
        BigDecimal balance = new BigDecimal("123.45");

        Account account = Account.builder()
                .userId(userId)
                .balance(balance)
                .build();

        when(accountRepository.findByUserId(userId)).thenReturn(Mono.just(account));

        StepVerifier.create(accountService.getBalance(userId))
                .expectNextMatches(response ->
                        response.getUserId().equals(userId.toString()) &&
                                response.getBalance().compareTo(balance) == 0
                )
                .verifyComplete();
    }

    @Test
    void getBalance_shouldFail_whenAccountNotFound() {
        Long userId = 2L;

        when(accountRepository.findByUserId(userId)).thenReturn(Mono.empty());

        StepVerifier.create(accountService.getBalance(userId))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException &&
                        ex.getMessage().equals("Account not found for userId=" + userId))
                .verify();
    }
}

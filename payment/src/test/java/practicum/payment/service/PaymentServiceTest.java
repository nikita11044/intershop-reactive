package practicum.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import practicum.payment.entity.Account;
import practicum.payment.exception.balance.InsufficientBalanceException;
import practicum.payment.r2dbc.AccountR2dbcRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private AccountR2dbcRepository accountRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_shouldSucceed_whenSufficientBalance() {
        Long userId = 1L;
        BigDecimal initialBalance = new BigDecimal("100.00");
        BigDecimal amountToPay = new BigDecimal("30.00");

        Account account = Account.builder()
                .userId(userId)
                .balance(initialBalance)
                .build();

        when(accountRepository.findByUserId(userId)).thenReturn(Mono.just(account));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(account));

        StepVerifier.create(paymentService.processPayment(userId, amountToPay))
                .verifyComplete();

        verify(accountRepository).save(argThat(saved ->
                saved.getBalance().compareTo(initialBalance.subtract(amountToPay)) == 0
        ));
    }

    @Test
    void processPayment_shouldFail_whenInsufficientBalance() {
        Long userId = 2L;
        BigDecimal initialBalance = new BigDecimal("10.00");
        BigDecimal amountToPay = new BigDecimal("50.00");

        Account account = Account.builder()
                .userId(userId)
                .balance(initialBalance)
                .build();

        when(accountRepository.findByUserId(userId)).thenReturn(Mono.just(account));

        StepVerifier.create(paymentService.processPayment(userId, amountToPay))
                .expectErrorMatches(ex -> ex instanceof InsufficientBalanceException &&
                        ex.getMessage().equals("Insufficient balance for userId=" + userId))
                .verify();

        verify(accountRepository, never()).save(any());
    }

    @Test
    void processPayment_shouldFail_whenAccountNotFound() {
        Long userId = 3L;

        when(accountRepository.findByUserId(userId)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.processPayment(userId, BigDecimal.ONE))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException &&
                        ex.getMessage().contains("Account not found for userId=" + userId))
                .verify();

        verify(accountRepository, never()).save(any());
    }
}

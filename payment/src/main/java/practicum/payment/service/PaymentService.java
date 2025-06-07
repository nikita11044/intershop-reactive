package practicum.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import practicum.payment.exception.balance.InsufficientBalanceException;
import practicum.payment.r2dbc.AccountR2dbcRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final AccountR2dbcRepository accountRepository;


    public Mono<Void> processPayment(Long userId, BigDecimal amount) {
        return accountRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Account not found for userId=" + userId))
                )
                .flatMap(account -> {
                    BigDecimal newBalance = account.getBalance().subtract(amount);
                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.error(
                                new InsufficientBalanceException(userId)
                        );
                    }
                    account.setBalance(newBalance);
                    return accountRepository.save(account).then();
                });
    }
}

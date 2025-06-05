package practicum.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import practicum.payment.r2dbc.AccountR2dbcRepository;
import practicum.payment.entity.Account;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountR2dbcRepository accountRepository;

    public Mono<BigDecimal> getBalance(Long userId) {
        return accountRepository.findByUserId(userId)
                .map(Account::getBalance);
    }

    @Transactional
    public Mono<Void> makePayment(Long userId, BigDecimal amount) {
        return accountRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Account not found")))
                .flatMap(account -> {
                    if (account.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new IllegalStateException("Insufficient balance"));
                    }
                    BigDecimal newBalance = account.getBalance().subtract(amount);
                    account.setBalance(newBalance);
                    return accountRepository.save(account).then();
                });
    }
}

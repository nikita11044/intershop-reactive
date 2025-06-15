package practicum.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import practicum.payment.model.BalanceResponse;
import practicum.payment.r2dbc.AccountR2dbcRepository;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountR2dbcRepository accountRepository;

    public Mono<BalanceResponse> getBalance(Long userId) {
        return accountRepository
                .findByUserId(userId)
                .map(account -> new BalanceResponse()
                        .userId(account.getUserId().toString())
                        .balance(account.getBalance())
                )
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Account not found for userId=" + userId)
                ));
    }
}

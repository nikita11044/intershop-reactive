package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import practicum.intershopreactive.client.BalanceClient;
import practicum.intershopreactive.model.BalanceResponse;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

@Service
@RequiredArgsConstructor
public class BalanceService {
    private final BalanceClient balanceClient;

    public Mono<BalanceResponse> getUserBalance(Long userId) {
        return balanceClient
                .getUserBalance(userId)
                .onErrorResume(WebClientRequestException.class, ex -> {
                    if (ex.getCause() instanceof ConnectException) {
                        return Mono.error(new IllegalStateException("Account service is unavailable", ex));
                    }
                    return Mono.error(ex);
                });
    }
}

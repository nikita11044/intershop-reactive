package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import practicum.intershopreactive.client.PaymentClient;
import practicum.intershopreactive.model.PaymentRequest;
import practicum.intershopreactive.model.PaymentResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.ConnectException;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentClient paymentClient;

    public Mono<PaymentResponse> processPayment(BigDecimal totalSum, Long userId) {
        var paymentRequest = new PaymentRequest()
                .amount(totalSum)
                .userId(userId);

        return paymentClient
                .processPayment(paymentRequest)
                .onErrorResume(WebClientRequestException.class, ex -> {
                    if (ex.getCause() instanceof ConnectException) {
                        return Mono.error(new IllegalStateException("Payment service is unavailable", ex));
                    }
                    return Mono.error(ex);
                });
    }
}

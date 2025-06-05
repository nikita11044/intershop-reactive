package practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import practicum.payment.model.PaymentRequest;
import practicum.payment.model.PaymentResponse;
import practicum.payment.service.PaymentService;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public Mono<ResponseEntity<PaymentResponse>> processPayment(@RequestBody PaymentRequest request) {
        Long userId = Long.valueOf(request.getUserId());
        BigDecimal amount = request.getAmount();

        return paymentService.processPayment(userId, amount)
                .thenReturn(ResponseEntity.ok(
                        new PaymentResponse()
                                .success(true)
                ));
    }
}

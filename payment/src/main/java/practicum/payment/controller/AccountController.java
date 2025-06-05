package practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import practicum.payment.service.AccountService;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{userId}/balance")
    public Mono<BigDecimal> getBalance(@PathVariable Long userId) {
        return accountService.getBalance(userId);
    }

    @PostMapping("/{userId}/pay")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> makePayment(@PathVariable Long userId, @RequestParam BigDecimal amount) {
        return accountService.makePayment(userId, amount);
    }
}

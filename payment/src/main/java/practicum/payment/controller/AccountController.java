package practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import practicum.payment.model.BalanceResponse;
import practicum.payment.service.AccountService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/balance")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<BalanceResponse> getBalance(@PathVariable Long userId) {
        return accountService.getBalance(userId);
    }
}

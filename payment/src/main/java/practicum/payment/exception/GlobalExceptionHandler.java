package practicum.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import practicum.payment.exception.balance.InsufficientBalanceException;
import practicum.payment.model.ErrorResponse;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleInsufficientFunds(InsufficientBalanceException ex) {
        return Mono.just(new ErrorResponse().error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.just(new ErrorResponse().error(ex.getMessage()));
    }
}

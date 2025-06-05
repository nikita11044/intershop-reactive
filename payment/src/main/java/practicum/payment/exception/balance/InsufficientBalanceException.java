package practicum.payment.exception.balance;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(Long userId) {
        super("Insufficient balance for userId=" + userId);
    }
}

package practicum.payment.r2dbc;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import practicum.payment.entity.Account;
import reactor.core.publisher.Mono;

public interface AccountR2dbcRepository extends R2dbcRepository<Account, Long> {
   Mono<Account> findByUserId(Long userId);
}

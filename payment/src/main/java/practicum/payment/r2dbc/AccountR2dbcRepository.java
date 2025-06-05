package practicum.payment.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import practicum.payment.entity.Account;
import reactor.core.publisher.Mono;

public interface AccountR2dbcRepository extends ReactiveCrudRepository<Account, Long> {

    Mono<Account> findByUserId(Long userId);
}

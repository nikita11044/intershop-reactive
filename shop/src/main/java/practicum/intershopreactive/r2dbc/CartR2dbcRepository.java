package practicum.intershopreactive.r2dbc;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import practicum.intershopreactive.entity.CartItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CartR2dbcRepository extends R2dbcRepository<CartItem, Long> {

    Mono<CartItem> findByProductIdAndUserId(Long productId, Long userId);

    Flux<CartItem> findByUserId(Long userId);

    @NotNull Mono<Void> deleteById(@NotNull Long id);

    @NotNull Mono<Void> deleteAllByUserId(@NotNull Long userId);
}

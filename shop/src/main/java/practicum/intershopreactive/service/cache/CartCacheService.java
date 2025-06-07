package practicum.intershopreactive.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CartCacheService {
    private final CartR2dbcRepository cartRepository;

    @Cacheable(value = "cartItems", key = "{#userId}")
    public Flux<CartItem> findByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    @CacheEvict(
            value = "cartItems",
            allEntries = true
    )
    public Mono<Void> evictCartItemsCache() {
        return Mono.empty();
    }
}

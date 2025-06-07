package practicum.intershopreactive.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProductCacheService {
    private final ProductR2dbcRepository productRepository;

    @Cacheable(value = "productsList", key = "{#search, #searchPattern, #sort, #limit, #offset}")
    public Flux<Product> findProducts(String search, String searchPattern, String sort, int limit, int offset) {
        return productRepository.findProducts(search, searchPattern, sort, limit, offset);
    }

    @Cacheable(value = "products", key = "#id")
    public Mono<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @CacheEvict(
            value = "productsList",
            allEntries = true
    )
    public Mono<Void> evictProductsCache() {
        return Mono.empty();
    }
}

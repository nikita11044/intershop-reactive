package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import practicum.intershopreactive.mapper.ProductMapper;
import practicum.intershopreactive.service.cache.CartCacheService;
import practicum.intershopreactive.service.cache.ProductCacheService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    // TODO: add user logic
    private static final long USER_ID = 1;

    private final CartR2dbcRepository cartRepository;

    private final CartCacheService cartCacheService;
    private final ProductCacheService productCacheService;

    @Transactional
    public Mono<Void> addProduct(Long productId) {
        return cartRepository
                .findByProductIdAndUserId(productId, USER_ID)
                .flatMap(cartItem -> {
                    cartItem.setCount(cartItem.getCount() + 1);
                    return cartRepository.save(cartItem);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    var cartItem = CartItem.builder()
                            .count(1L)
                            .userId(USER_ID)
                            .productId(productId)
                            .build();

                    return cartRepository.save(cartItem);
                }))
                .then(Mono.when(
                        cartCacheService.evictCartItemsCache(),
                        productCacheService.evictProductsCache()
                ));
    }

    @Transactional
    public Mono<Void> removeProduct(Long productId) {
        return cartRepository
                .findByProductIdAndUserId(productId, USER_ID)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Product not found")))
                .flatMap(cartItem -> {
                    long currentCount = cartItem.getCount();
                    if (currentCount > 1) {
                        cartItem.setCount(currentCount - 1);
                    } else {
                       return cartRepository.deleteById(cartItem.getId());
                    }
                    return cartRepository.save(cartItem);
                })
                .then(Mono.when(
                        cartCacheService.evictCartItemsCache(),
                        productCacheService.evictProductsCache()
                ));
    }

    @Transactional
    public Mono<Void> deleteProduct(Long productId) {
        return cartRepository
                .findByProductIdAndUserId(productId, USER_ID)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Product not found")))
                .flatMap(cartItem -> cartRepository.deleteById(cartItem.getId()))
                .then(Mono.when(
                        cartCacheService.evictCartItemsCache(),
                        productCacheService.evictProductsCache()
                ));
    }

    public Flux<ProductDto> getAllCartItems() {
        return cartCacheService
                .findByUserId(USER_ID)
                .collectList()
                .flatMapMany(cartItems -> {
                    List<Long> productIds = cartItems.stream()
                            .map(CartItem::getProductId)
                            .distinct()
                            .collect(Collectors.toList());

                    return Flux.fromIterable(productIds)
                            .flatMap(productCacheService::findById)
                            .collectMap(Product::getId)
                            .flatMapMany(productMap -> Flux.fromIterable(cartItems)
                                    .map(cartItem -> {
                                        Product product = productMap.get(cartItem.getProductId());
                                        return ProductDto.builder()
                                                .id(product.getId())
                                                .title(product.getTitle())
                                                .description(product.getDescription())
                                                .imgPath(product.getImgPath())
                                                .price(product.getPrice())
                                                .count(cartItem.getCount().intValue())
                                                .build();
                                    })
                            );
                });
    }

}

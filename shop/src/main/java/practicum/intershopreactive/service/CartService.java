package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import practicum.intershopreactive.dto.cart.CartDto;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.service.cache.CartCacheService;
import practicum.intershopreactive.service.cache.ProductCacheService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CartService {
    // TODO: add user logic
    private static final long USER_ID = 1;

    private final CartR2dbcRepository cartRepository;

    private final CartCacheService cartCacheService;
    private final ProductCacheService productCacheService;
    private final BalanceService balanceService;

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

    public Mono<CartDto> getAllCartItems() {
        return cartCacheService.findByUserId(USER_ID)
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.just(
                                CartDto.builder()
                                        .items(Collections.emptyList())
                                        .empty(true)
                                        .total(BigDecimal.ZERO)
                                        .canBuy(false)
                                        .available(true)
                                        .build()
                        );
                    }

                    List<Long> productIds = cartItems.stream()
                            .map(CartItem::getProductId)
                            .distinct()
                            .toList();

                    return Flux.fromIterable(productIds)
                            .flatMap(productCacheService::findById)
                            .collectMap(Product::getId)
                            .flatMap(productMap -> {
                                List<ProductDto> productDtos = cartItems.stream()
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
                                        .toList();

                                BigDecimal total = productDtos.stream()
                                        .map(dto -> dto.getPrice().multiply(BigDecimal.valueOf(dto.getCount())))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                return balanceService.getUserBalance(USER_ID)
                                        .map(balanceResponse -> {
                                            boolean canBuy = balanceResponse.getBalance().compareTo(total) >= 0;
                                            return CartDto.builder()
                                                    .items(productDtos)
                                                    .empty(false)
                                                    .total(total)
                                                    .canBuy(canBuy)
                                                    .available(true)
                                                    .build();
                                        })
                                        .onErrorResume(ex -> Mono.just(
                                                CartDto.builder()
                                                        .items(productDtos)
                                                        .empty(false)
                                                        .total(total)
                                                        .canBuy(false)
                                                        .available(false)
                                                        .build()
                                        ));
                            });
                });
    }


}

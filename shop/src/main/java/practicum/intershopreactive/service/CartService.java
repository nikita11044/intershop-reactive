package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import practicum.intershopreactive.mapper.ProductMapper;
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

    private final ProductR2dbcRepository productRepository;
    private final CartR2dbcRepository cartRepository;

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
                .then();
    }

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
                .then();
    }

    public Mono<Void> deleteProduct(Long productId) {
        return cartRepository
                .findByProductIdAndUserId(productId, USER_ID)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Product not found")))
                .flatMap(cartItem -> {
                    return cartRepository.deleteById(cartItem.getId());
                })
                .then();
    }

    public Flux<ProductDto> getAllCartItems() {
        return cartRepository.findByUserId(USER_ID)
                .collectList()
                .flatMapMany(cartItems -> {
                    List<Long> productIds = cartItems.stream()
                            .map(CartItem::getProductId)
                            .distinct()
                            .collect(Collectors.toList());

                    return productRepository.findAllById(productIds)
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

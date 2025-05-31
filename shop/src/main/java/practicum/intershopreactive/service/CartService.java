package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import practicum.intershopreactive.mapper.ProductMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CartService {
    private final ProductR2dbcRepository productRepository;
    private final ProductMapper productMapper;

    public Mono<Void> addProduct(Long productId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Product not found")))
                .flatMap(product -> {
                    product.setCount(product.getCount() + 1);
                    return productRepository.save(product);
                })
                .then();
    }

    public Mono<Void> removeProduct(Long productId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Product not found")))
                .flatMap(product -> {
                    int currentCount = product.getCount();
                    if (currentCount > 1) {
                        product.setCount(currentCount - 1);
                    } else {
                        product.setCount(0);
                    }
                    return productRepository.save(product);
                })
                .then();
    }

    public Mono<Void> deleteProduct(Long productId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Product not found")))
                .flatMap(product -> {
                    product.setCount(0);
                    return productRepository.save(product);
                })
                .then();
    }

    public Flux<ProductDto> getAllCartItems() {
        return productRepository.findAll()
                .filter(product -> product.getCount() > 0)
                .map(productMapper::toDto);
    }
}

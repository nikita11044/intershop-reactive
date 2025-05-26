package practicum.intershopreactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.mapper.ProductMapper;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class CartServiceTest {

    @Mock
    private ProductR2dbcRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CartService cartService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .title("Sample Product")
                .count(1)
                .build();
    }

    @Test
    void testAddProduct_productExists() {
        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));

        StepVerifier.create(cartService.addProduct(1L))
                .verifyComplete();

        verify(productRepository, times(1)).save(argThat(p -> p.getCount() == 2));
    }

    @Test
    void testAddProduct_productNotFound() {
        when(productRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.addProduct(1L))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(productRepository, never()).save(any());
    }

    @Test
    void testRemoveProduct_countGreaterThanOne() {
        product.setCount(3);
        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));

        StepVerifier.create(cartService.removeProduct(1L))
                .verifyComplete();

        verify(productRepository, times(1)).save(argThat(p -> p.getCount() == 2));
    }

    @Test
    void testRemoveProduct_countEqualsOne() {
        product.setCount(1);
        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));

        StepVerifier.create(cartService.removeProduct(1L))
                .verifyComplete();

        verify(productRepository, times(1)).save(argThat(p -> p.getCount() == 0));
    }

    @Test
    void testRemoveProduct_productNotFound() {
        when(productRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.removeProduct(1L))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(productRepository, never()).save(any());
    }

    @Test
    void testDeleteProduct_productExists() {
        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));

        StepVerifier.create(cartService.deleteProduct(1L))
                .verifyComplete();

        verify(productRepository, times(1)).save(argThat(p -> p.getCount() == 0));
    }

    @Test
    void testDeleteProduct_productNotFound() {
        when(productRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.deleteProduct(1L))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(productRepository, never()).save(any());
    }

    @Test
    void testGetAllCartItems_withItems() {
        Product product1 = Product.builder()
                .id(1L)
                .title("Product 1")
                .count(1)
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .title("Product 2")
                .count(0)
                .build();

        when(productRepository.findAll()).thenReturn(Flux.just(product1, product2));

        ProductDto dto1 = ProductDto.builder()
                .id(1L)
                .title("Product 1")
                .build();

        when(productMapper.toDto(product1)).thenReturn(dto1);

        StepVerifier.create(cartService.getAllCartItems())
                .expectNext(dto1)
                .verifyComplete();
    }

    @Test
    void testGetAllCartItems_noItems() {
        Product product1 = Product.builder()
                .id(1L)
                .title("Product 1")
                .count(0)
                .build();

        when(productRepository.findAll()).thenReturn(Flux.just(product1));

        StepVerifier.create(cartService.getAllCartItems())
                .verifyComplete();
    }
}

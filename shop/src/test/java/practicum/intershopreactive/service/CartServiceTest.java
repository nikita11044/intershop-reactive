package practicum.intershopreactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private ProductR2dbcRepository productRepository;

    @Mock
    private CartR2dbcRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    private static final long USER_ID = 1L;
    private static final long PRODUCT_ID = 1L;

    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        cartItem = CartItem.builder()
                .id(1L)
                .userId(USER_ID)
                .productId(PRODUCT_ID)
                .count(1L)
                .build();
    }

    @Test
    void testAddProduct_existingCartItem() {
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.just(cartItem));
        when(cartRepository.save(any(CartItem.class))).thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.addProduct(PRODUCT_ID))
                .verifyComplete();

        verify(cartRepository, times(1)).save(argThat(item -> item.getCount() == 2L));
    }

    @Test
    void testAddProduct_newCartItem() {
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.empty());
        when(cartRepository.save(any(CartItem.class))).thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.addProduct(PRODUCT_ID))
                .verifyComplete();

        verify(cartRepository, times(1)).save(argThat(item ->
                item.getProductId().equals(PRODUCT_ID) &&
                        item.getUserId().equals(USER_ID) &&
                        item.getCount() == 1L
        ));
    }

    @Test
    void testRemoveProduct_countGreaterThanOne() {
        cartItem.setCount(3L);
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.just(cartItem));
        when(cartRepository.save(any(CartItem.class))).thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.removeProduct(PRODUCT_ID))
                .verifyComplete();

        verify(cartRepository, times(1)).save(argThat(item -> item.getCount() == 2L));
    }

    @Test
    void testRemoveProduct_countEqualsOne() {
        cartItem.setCount(1L);
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.just(cartItem));
        when(cartRepository.deleteById(cartItem.getId())).thenReturn(Mono.empty());

        StepVerifier.create(cartService.removeProduct(PRODUCT_ID))
                .verifyComplete();

        verify(cartRepository, times(1)).deleteById(cartItem.getId());
    }

    @Test
    void testRemoveProduct_productNotFound() {
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.removeProduct(PRODUCT_ID))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(cartRepository, never()).save(any());
        verify(cartRepository, never()).deleteById((Long) any());
    }

    @Test
    void testDeleteProduct_productExists() {
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.just(cartItem));
        when(cartRepository.deleteById(cartItem.getId())).thenReturn(Mono.empty());

        StepVerifier.create(cartService.deleteProduct(PRODUCT_ID))
                .verifyComplete();

        verify(cartRepository, times(1)).deleteById(cartItem.getId());
    }

    @Test
    void testDeleteProduct_productNotFound() {
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.deleteProduct(PRODUCT_ID))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(cartRepository, never()).deleteById((Long) any());
    }

    @Test
    void testGetAllCartItems_withItems() {
        CartItem cartItem1 = CartItem.builder()
                .id(1L)
                .userId(USER_ID)
                .productId(1L)
                .count(2L)
                .build();

        CartItem cartItem2 = CartItem.builder()
                .id(2L)
                .userId(USER_ID)
                .productId(2L)
                .count(1L)
                .build();

        Product product1 = Product.builder()
                .id(1L)
                .title("Product 1")
                .description("Description 1")
                .imgPath("/images/product1.jpg")
                .price(BigDecimal.valueOf(10.0))
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .title("Product 2")
                .description("Description 2")
                .imgPath("/images/product2.jpg")
                .price(BigDecimal.valueOf(20.0))
                .build();

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Flux.just(cartItem1, cartItem2));
        when(productRepository.findAllById(List.of(1L, 2L))).thenReturn(Flux.just(product1, product2));

        StepVerifier.create(cartService.getAllCartItems())
                .expectNextMatches(dto ->
                        dto.getId().equals(1L) &&
                                dto.getTitle().equals("Product 1") &&
                                dto.getCount() == 2
                )
                .expectNextMatches(dto ->
                        dto.getId().equals(2L) &&
                                dto.getTitle().equals("Product 2") &&
                                dto.getCount() == 1
                )
                .verifyComplete();
    }
}

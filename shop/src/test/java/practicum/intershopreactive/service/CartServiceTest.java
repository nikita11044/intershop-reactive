package practicum.intershopreactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.model.BalanceResponse;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.service.cache.CartCacheService;
import practicum.intershopreactive.service.cache.ProductCacheService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartR2dbcRepository cartRepository;

    @Mock
    private CartCacheService cartCacheService;

    @Mock
    private ProductCacheService productCacheService;

    @Mock
    private BalanceService balanceService;

    @Mock
    private UserService userService;

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
        when(cartCacheService.evictCartItemsCache()).thenReturn(Mono.empty());
        when(productCacheService.evictProductsCache()).thenReturn(Mono.empty());
        when(userService.getCurrentUserId()).thenReturn(Mono.just(USER_ID));

        StepVerifier.create(cartService.addProduct(PRODUCT_ID))
                .verifyComplete();

        verify(cartRepository, times(1)).save(argThat(item -> item.getCount() == 2L));
    }

    @Test
    void testAddProduct_newCartItem() {
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.empty());
        when(cartRepository.save(any(CartItem.class))).thenReturn(Mono.just(cartItem));
        when(cartCacheService.evictCartItemsCache()).thenReturn(Mono.empty());
        when(productCacheService.evictProductsCache()).thenReturn(Mono.empty());
        when(userService.getCurrentUserId()).thenReturn(Mono.just(USER_ID));

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
        when(cartCacheService.evictCartItemsCache()).thenReturn(Mono.empty());
        when(productCacheService.evictProductsCache()).thenReturn(Mono.empty());
        when(userService.getCurrentUserId()).thenReturn(Mono.just(USER_ID));

        StepVerifier.create(cartService.removeProduct(PRODUCT_ID))
                .verifyComplete();

        verify(cartRepository, times(1)).save(argThat(item -> item.getCount() == 2L));
    }

    @Test
    void testRemoveProduct_countEqualsOne() {
        cartItem.setCount(1L);
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.just(cartItem));
        when(cartRepository.deleteById(cartItem.getId())).thenReturn(Mono.empty());
        when(cartCacheService.evictCartItemsCache()).thenReturn(Mono.empty());
        when(productCacheService.evictProductsCache()).thenReturn(Mono.empty());
        when(userService.getCurrentUserId()).thenReturn(Mono.just(USER_ID));

        StepVerifier.create(cartService.removeProduct(PRODUCT_ID))
                .verifyComplete();

        verify(cartRepository, times(1)).deleteById(cartItem.getId());
    }

    @Test
    void testRemoveProduct_productNotFound() {
        when(userService.getCurrentUserId()).thenReturn(Mono.just(USER_ID));
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.removeProduct(PRODUCT_ID))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    void testDeleteProduct_productExists() {
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.just(cartItem));
        when(cartRepository.deleteById(cartItem.getId())).thenReturn(Mono.empty());
        when(cartCacheService.evictCartItemsCache()).thenReturn(Mono.empty());
        when(productCacheService.evictProductsCache()).thenReturn(Mono.empty());
        when(userService.getCurrentUserId()).thenReturn(Mono.just(USER_ID));

        StepVerifier.create(cartService.deleteProduct(PRODUCT_ID))
                .verifyComplete();
    }

    @Test
    void testDeleteProduct_productNotFound() {
        when(cartRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Mono.empty());
        when(userService.getCurrentUserId()).thenReturn(Mono.just(USER_ID));

        StepVerifier.create(cartService.deleteProduct(PRODUCT_ID))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    void testGetAllCartItems_withProductsAndBalance() {
        CartItem item1 = CartItem.builder().id(1L).userId(USER_ID).productId(1L).count(2L).build();
        CartItem item2 = CartItem.builder().id(2L).userId(USER_ID).productId(2L).count(1L).build();

        Product product1 = Product.builder().id(1L).title("P1").description("D1").imgPath("/img1").price(BigDecimal.valueOf(100)).build();
        Product product2 = Product.builder().id(2L).title("P2").description("D2").imgPath("/img2").price(BigDecimal.valueOf(50)).build();

        when(cartCacheService.findByUserId(USER_ID)).thenReturn(Flux.just(item1, item2));
        when(productCacheService.findById(1L)).thenReturn(Mono.just(product1));
        when(productCacheService.findById(2L)).thenReturn(Mono.just(product2));
        when(balanceService.getUserBalance(USER_ID)).thenReturn(Mono.just(new BalanceResponse().balance(BigDecimal.valueOf(200)).userId(USER_ID)));
        when(userService.getCurrentUserId()).thenReturn(Mono.just(USER_ID));

        StepVerifier.create(cartService.getAllCartItems())
                .expectNextMatches(cartDto ->
                        !cartDto.isEmpty() &&
                                cartDto.getItems().size() == 2 &&
                                cartDto.getTotal().equals(BigDecimal.valueOf(250)) &&
                                !cartDto.isCanBuy() &&
                                cartDto.isAvailable()
                )
                .verifyComplete();
    }

    @Test
    void testGetAllCartItems_empty() {
        when(cartCacheService.findByUserId(USER_ID)).thenReturn(Flux.empty());
        when(userService.getCurrentUserId()).thenReturn(Mono.just(USER_ID));

        StepVerifier.create(cartService.getAllCartItems())
                .expectNextMatches(cartDto ->
                        cartDto.isEmpty() &&
                                cartDto.getItems().isEmpty() &&
                                cartDto.getTotal().compareTo(BigDecimal.ZERO) == 0 &&
                                !cartDto.isCanBuy() &&
                                cartDto.isAvailable()
                )
                .verifyComplete();
    }

    @Test
    void testGetAllCartItems_balanceErrorHandled() {
        CartItem item = CartItem.builder().id(1L).userId(USER_ID).productId(1L).count(1L).build();
        Product product = Product.builder().id(1L).title("P").description("D").imgPath("/img").price(BigDecimal.valueOf(100)).build();

        when(cartCacheService.findByUserId(USER_ID)).thenReturn(Flux.just(item));
        when(productCacheService.findById(1L)).thenReturn(Mono.just(product));
        when(balanceService.getUserBalance(USER_ID)).thenReturn(Mono.error(new RuntimeException("Balance service down")));
        when(userService.getCurrentUserId()).thenReturn(Mono.just(USER_ID));

        StepVerifier.create(cartService.getAllCartItems())
                .expectNextMatches(cartDto ->
                        !cartDto.isEmpty() &&
                                cartDto.getItems().size() == 1 &&
                                cartDto.getTotal().equals(BigDecimal.valueOf(100)) &&
                                !cartDto.isCanBuy() &&
                                !cartDto.isAvailable()
                )
                .verifyComplete();
    }
}

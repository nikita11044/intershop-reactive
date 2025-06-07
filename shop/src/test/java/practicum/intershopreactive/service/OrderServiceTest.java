package practicum.intershopreactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.reactive.TransactionalOperator;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Order;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.model.PaymentResponse;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.r2dbc.OrderItemR2dbcRepository;
import practicum.intershopreactive.r2dbc.OrderR2dbcRepository;
import practicum.intershopreactive.service.cache.CartCacheService;
import practicum.intershopreactive.service.cache.ProductCacheService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @Mock
    private OrderR2dbcRepository orderRepository;

    @Mock
    private CartR2dbcRepository cartRepository;

    @Mock
    private OrderItemR2dbcRepository orderItemRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private ProductCacheService productCacheService;

    @Mock
    private CartCacheService cartCacheService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    private Product product1;
    private Product product2;
    private CartItem cartItem1;
    private CartItem cartItem2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        product1 = Product.builder()
                .id(1L)
                .title("Product 1")
                .description("Description 1")
                .imgPath("img1.jpg")
                .price(BigDecimal.TEN)
                .build();

        product2 = Product.builder()
                .id(2L)
                .title("Product 2")
                .description("Description 2")
                .imgPath("img2.jpg")
                .price(new BigDecimal("20.00"))
                .build();

        cartItem1 = CartItem.builder()
                .id(1L)
                .userId(1L)
                .productId(1L)
                .count(2L)
                .build();

        cartItem2 = CartItem.builder()
                .id(2L)
                .userId(1L)
                .productId(2L)
                .count(1L)
                .build();

        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testPurchaseCart_successfulPurchase() {
        List<CartItem> cartItems = List.of(cartItem1, cartItem2);

        Order savedOrder = Order.builder()
                .id(100L)
                .createdAt(Instant.now())
                .userId(1L)
                .totalSum(new BigDecimal("40.00"))
                .build();

        when(cartCacheService.findByUserId(1L)).thenReturn(Flux.fromIterable(cartItems));
        when(productCacheService.findById(1L)).thenReturn(Mono.just(product1));
        when(productCacheService.findById(2L)).thenReturn(Mono.just(product2));
        when(paymentService.processPayment(new BigDecimal("40.00"), 1L))
                .thenReturn(Mono.just(new PaymentResponse().success(true)));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.empty());
        when(cartRepository.deleteAllByUserId(1L)).thenReturn(Mono.empty());
        when(cartCacheService.evictCartItemsCache()).thenReturn(Mono.empty());
        when(productCacheService.evictProductsCache()).thenReturn(Mono.empty());

        StepVerifier.create(orderService.purchaseCart())
                .expectNext(100L)
                .verifyComplete();

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
        verify(cartRepository).deleteAllByUserId(1L);
        verify(paymentService).processPayment(new BigDecimal("40.00"), 1L);
    }

    @Test
    void testPurchaseCart_emptyCart() {
        when(cartCacheService.findByUserId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(orderService.purchaseCart())
                .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                        ex.getMessage().equals("Cart is empty, cannot create order"))
                .verify();

        verifyNoInteractions(orderRepository, orderItemRepository, paymentService);
    }

    @Test
    void testPurchaseCart_productNotFound() {
        when(cartCacheService.findByUserId(1L)).thenReturn(Flux.just(cartItem1));
        when(productCacheService.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.purchaseCart())
                .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                        ex.getMessage().contains("Product not found"))
                .verify();
    }

    @Test
    void testPurchaseCart_paymentFails() {
        when(cartCacheService.findByUserId(1L)).thenReturn(Flux.just(cartItem1));
        when(productCacheService.findById(1L)).thenReturn(Mono.just(product1));
        when(paymentService.processPayment(any(), anyLong()))
                .thenReturn(Mono.just(new PaymentResponse().success(false)));

        StepVerifier.create(orderService.purchaseCart())
                .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                        ex.getMessage().equals("Payment failed"))
                .verify();
    }
}

package practicum.intershopreactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.reactive.TransactionalOperator;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Order;
import practicum.intershopreactive.entity.OrderItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.r2dbc.OrderItemR2dbcRepository;
import practicum.intershopreactive.r2dbc.OrderR2dbcRepository;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock
    private OrderR2dbcRepository orderRepository;

    @Mock
    private ProductR2dbcRepository productRepository;

    @Mock
    private CartR2dbcRepository cartRepository;

    @Mock
    private OrderItemR2dbcRepository orderItemRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

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
        List<Product> products = List.of(product1, product2);

        Order savedOrder = Order.builder()
                .id(100L)
                .createdAt(Instant.now())
                .userId(1L)
                .totalSum(new BigDecimal("40.00"))
                .build();

        when(cartRepository.findByUserId(1L))
                .thenReturn(Flux.fromIterable(cartItems));
        when(productRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(Flux.fromIterable(products));
        when(orderRepository.save(any(Order.class)))
                .thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.saveAll(anyList()))
                .thenReturn(Flux.empty());
        when(cartRepository.deleteAllByUserId(1L))
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.purchaseCart())
                .expectNext(100L)
                .verifyComplete();

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).saveAll(anyList());
        verify(cartRepository, times(1)).deleteAllByUserId(1L);
    }

    @Test
    void testPurchaseCart_emptyCart() {
        when(cartRepository.findByUserId(1L))
                .thenReturn(Flux.empty());

        StepVerifier.create(orderService.purchaseCart())
                .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                        ex.getMessage().equals("Cart is empty, cannot create order"))
                .verify();

        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).saveAll(anyList());
        verify(cartRepository, never()).deleteAllByUserId(anyLong());
    }

    @Test
    void testGetAllOrders_returnsOrderDtos() {
        Order order = Order.builder()
                .id(1L)
                .createdAt(Instant.now())
                .userId(1L)
                .totalSum(new BigDecimal("30.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .orderId(1L)
                .productId(1L)
                .quantity(2L)
                .priceAtPurchase(BigDecimal.TEN)
                .build();

        Product product = Product.builder()
                .id(1L)
                .title("Test Product")
                .description("Desc")
                .imgPath("img.jpg")
                .price(BigDecimal.TEN)
                .build();

        when(orderRepository.findAll())
                .thenReturn(Flux.just(order));
        when(orderItemRepository.findByOrderId(1L))
                .thenReturn(Flux.just(item));
        when(productRepository.findById(1L))
                .thenReturn(Mono.just(product));

        StepVerifier.create(orderService.findAllWithItemsAndProducts())
                .expectNextMatches(dto -> dto.getId() == 1L &&
                        dto.getTotalSum().compareTo(new BigDecimal("20.00")) == 0 &&
                        dto.getItems().size() == 1)
                .verifyComplete();
    }

    @Test
    void testGetOrderById_orderExists() {
        Order order = Order.builder()
                .id(1L)
                .createdAt(Instant.now())
                .userId(1L)
                .totalSum(new BigDecimal("30.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .orderId(1L)
                .productId(1L)
                .quantity(2L)
                .priceAtPurchase(BigDecimal.TEN)
                .build();

        Product product = Product.builder()
                .id(1L)
                .title("Product 1")
                .description("Desc")
                .imgPath("img.jpg")
                .price(BigDecimal.TEN)
                .build();

        when(orderRepository.findById(1L))
                .thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L))
                .thenReturn(Flux.just(item));
        when(productRepository.findById(1L))
                .thenReturn(Mono.just(product));

        StepVerifier.create(orderService.getOrderById(1L))
                .expectNextMatches(dto -> dto.getId() == 1L &&
                        dto.getItems().size() == 1 &&
                        dto.getItems().get(0).getProductId() == 1L)
                .verifyComplete();
    }

    @Test
    void testGetOrderById_orderNotFound() {
        when(orderRepository.findById(1L))
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrderById(1L))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException &&
                        ex.getMessage().equals("Order not found with id: 1"))
                .verify();
    }
}

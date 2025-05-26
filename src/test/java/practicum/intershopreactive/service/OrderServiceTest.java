package practicum.intershopreactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.reactive.TransactionalOperator;
import practicum.intershopreactive.entity.Order;
import practicum.intershopreactive.entity.OrderItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.r2dbc.OrderItemR2dbcRepository;
import practicum.intershopreactive.r2dbc.OrderR2dbcRepository;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @Mock
    private OrderR2dbcRepository orderRepository;

    @Mock
    private ProductR2dbcRepository productRepository;

    @Mock
    private OrderItemR2dbcRepository orderItemRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private OrderService orderService;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        product1 = Product.builder()
                .id(1L)
                .title("Product 1")
                .price(BigDecimal.TEN)
                .count(2)
                .build();

        product2 = Product.builder()
                .id(2L)
                .title("Product 2")
                .price(new BigDecimal("20.00"))
                .count(1)
                .build();

        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testPurchaseCart_successfulPurchase() {
        List<Product> productsInCart = List.of(product1, product2);

        Order savedOrder = Order.builder()
                .id(100L)
                .createdAt(Instant.now())
                .totalSum(new BigDecimal("40.00"))
                .build();

        when(productRepository.findByCountGreaterThan(0))
                .thenReturn(Flux.fromIterable(productsInCart));
        when(orderRepository.save(any(Order.class)))
                .thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.saveAll(anyList()))
                .thenReturn(Flux.empty());
        when(productRepository.save(any(Product.class)))
                .thenReturn(Mono.just(product1), Mono.just(product2));

        StepVerifier.create(orderService.purchaseCart())
                .expectNext(100L)
                .verifyComplete();

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).saveAll(anyList());
        verify(productRepository, times(2)).save(any(Product.class));
    }

    @Test
    void testPurchaseCart_emptyCart() {
        when(productRepository.findByCountGreaterThan(0))
                .thenReturn(Flux.empty());

        StepVerifier.create(orderService.purchaseCart())
                .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                        ex.getMessage().equals("Cart is empty, cannot create order"))
                .verify();

        verify(orderRepository, never()).save(any());
    }

    @Test
    void testGetAllOrders_returnsOrderDtos() {
        Order order = Order.builder()
                .id(1L)
                .createdAt(Instant.now())
                .totalSum(new BigDecimal("30.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .orderId(1L)
                .productId(1L)
                .quantity(2)
                .priceAtPurchase(BigDecimal.TEN)
                .build();

        Product product = Product.builder()
                .id(1L)
                .title("Test Product")
                .description("Desc")
                .imgPath("img.jpg")
                .count(5)
                .price(BigDecimal.TEN)
                .build();

        when(orderRepository.findAll())
                .thenReturn(Flux.just(order));
        when(orderItemRepository.findByOrderId(1L))
                .thenReturn(Flux.just(item));
        when(productRepository.findById(1L))
                .thenReturn(Mono.just(product));

        StepVerifier.create(orderService.findAllWithItemsAndProducts())
                .expectNextMatches(dto -> dto.getId() == 1L && dto.getTotalSum().compareTo(new BigDecimal("20.00")) == 0)
                .verifyComplete();
    }

    @Test
    void testGetOrderById_orderExists() {
        Order order = Order.builder()
                .id(1L)
                .createdAt(Instant.now())
                .totalSum(new BigDecimal("30.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .orderId(1L)
                .productId(1L)
                .quantity(2)
                .priceAtPurchase(BigDecimal.TEN)
                .build();

        Product product = Product.builder()
                .id(1L)
                .title("Product 1")
                .description("Desc")
                .imgPath("img.jpg")
                .price(BigDecimal.TEN)
                .count(5)
                .build();

        when(orderRepository.findById(1L))
                .thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L))
                .thenReturn(Flux.just(item));
        when(productRepository.findById(1L))
                .thenReturn(Mono.just(product));

        StepVerifier.create(orderService.getOrderById(1L))
                .expectNextMatches(dto -> dto.getId() == 1L && dto.getItems().size() == 1)
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

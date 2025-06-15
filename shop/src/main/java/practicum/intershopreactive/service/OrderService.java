package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import practicum.intershopreactive.dto.order.OrderDto;
import practicum.intershopreactive.dto.order.OrderItemDto;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Order;
import practicum.intershopreactive.entity.OrderItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.r2dbc.OrderItemR2dbcRepository;
import practicum.intershopreactive.r2dbc.OrderR2dbcRepository;
import practicum.intershopreactive.service.cache.CartCacheService;
import practicum.intershopreactive.service.cache.ProductCacheService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderR2dbcRepository orderRepository;
    private final CartR2dbcRepository cartRepository;
    private final OrderItemR2dbcRepository orderItemRepository;
    private final TransactionalOperator transactionalOperator;
    private final ProductCacheService productCacheService;
    private final CartCacheService cartCacheService;
    private final PaymentService paymentService;
    private final UserService userService;

    @Transactional
    public Mono<Long> purchaseCart() {
        return userService.getCurrentUserId()
                .flatMap(userId ->
                        cartCacheService.findByUserId(userId)
                                .collectList()
                                .flatMap(cartItems -> {
                                    if (cartItems.isEmpty()) {
                                        return Mono.error(new IllegalStateException("Cart is empty, cannot create order"));
                                    }

                                    List<Long> productIds = cartItems.stream()
                                            .map(CartItem::getProductId)
                                            .toList();

                                    return Flux.fromIterable(productIds)
                                            .flatMap(productCacheService::findById)
                                            .collectList()
                                            .flatMap(products -> {
                                                Map<Long, Product> productMap = products.stream()
                                                        .collect(Collectors.toMap(Product::getId, Function.identity()));

                                                List<OrderItem> orderItems = new ArrayList<>();
                                                BigDecimal totalSum = BigDecimal.ZERO;

                                                for (CartItem cartItem : cartItems) {
                                                    Product product = productMap.get(cartItem.getProductId());
                                                    if (product == null) {
                                                        return Mono.error(new IllegalStateException("Product not found: " + cartItem.getProductId()));
                                                    }

                                                    BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getCount()));
                                                    totalSum = totalSum.add(itemTotal);

                                                    OrderItem orderItem = OrderItem.builder()
                                                            .orderId(null)
                                                            .productId(product.getId())
                                                            .quantity(cartItem.getCount())
                                                            .priceAtPurchase(product.getPrice())
                                                            .build();
                                                    orderItems.add(orderItem);
                                                }

                                                Order order = Order.builder()
                                                        .createdAt(Instant.now())
                                                        .userId(userId)
                                                        .totalSum(totalSum)
                                                        .build();

                                                return paymentService.processPayment(order.getTotalSum(), order.getUserId())
                                                        .flatMap(response -> {
                                                            if (Boolean.TRUE.equals(response.getSuccess())) {
                                                                return orderRepository.save(order)
                                                                        .flatMap(savedOrder -> {
                                                                            orderItems.forEach(item -> item.setOrderId(savedOrder.getId()));

                                                                            Mono<Void> saveOrderItems = orderItemRepository.saveAll(orderItems).then();
                                                                            Mono<Void> deleteCartItems = cartRepository.deleteAllByUserId(userId);

                                                                            return Mono.when(saveOrderItems, deleteCartItems)
                                                                                    .then(Mono.when(
                                                                                            cartCacheService.evictCartItemsCache(),
                                                                                            productCacheService.evictProductsCache()
                                                                                    ))
                                                                                    .thenReturn(savedOrder.getId());
                                                                        });
                                                            } else {
                                                                return Mono.error(new IllegalStateException("Payment failed"));
                                                            }
                                                        });
                                            });
                                })
                )
                .as(transactionalOperator::transactional);
    }

    public Flux<OrderDto> findAllWithItemsAndProducts() {
        return orderRepository.findAll()
                .collectList()
                .flatMapMany(orders -> {
                    List<Mono<OrderDto>> orderDtoMonos = orders.stream()
                            .map(this::mapToOrderDto)
                            .toList();

                    return Mono.zip(orderDtoMonos, results ->
                                    Arrays.stream(results)
                                            .map(o -> (OrderDto) o)
                                            .toList()
                            )
                            .flatMapMany(Flux::fromIterable);
                });
    }

    private Mono<OrderDto> mapToOrderDto(Order order) {
        return orderItemRepository.findByOrderId(order.getId())
                .flatMap(orderItem -> {
                    Mono<Product> productMono = productCacheService.findById(orderItem.getProductId());
                    return Mono.zip(Mono.just(orderItem), productMono)
                            .map(tuple -> {
                                OrderItem oi = tuple.getT1();
                                Product p = tuple.getT2();
                                return OrderItemDto.builder()
                                        .id(oi.getId())
                                        .productId(p.getId())
                                        .title(p.getTitle())
                                        .description(p.getDescription())
                                        .imgPath(p.getImgPath())
                                        .quantity(oi.getQuantity())
                                        .priceAtPurchase(oi.getPriceAtPurchase())
                                        .build();
                            });
                })
                .collectList()
                .map(orderItemDtos -> {
                    BigDecimal totalSum = orderItemDtos.stream()
                            .map(item -> item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return OrderDto.builder()
                            .id(order.getId())
                            .items(orderItemDtos)
                            .totalSum(totalSum)
                            .build();
                });
    }

    public Mono<OrderDto> getOrderById(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found with id: " + id)))
                .flatMap(order ->
                        orderItemRepository.findByOrderId(order.getId())
                                .flatMap(orderItem ->
                                        productCacheService.findById(orderItem.getProductId())
                                                .map(product -> OrderItemDto.builder()
                                                        .id(orderItem.getId())
                                                        .productId(product.getId())
                                                        .title(product.getTitle())
                                                        .description(product.getDescription())
                                                        .imgPath(product.getImgPath())
                                                        .quantity(orderItem.getQuantity())
                                                        .priceAtPurchase(orderItem.getPriceAtPurchase())
                                                        .build())
                                )
                                .collectList()
                                .map(orderItemDtos -> OrderDto.builder()
                                        .id(order.getId())
                                        .totalSum(order.getTotalSum())
                                        .items(orderItemDtos)
                                        .build())
                );
    }
}

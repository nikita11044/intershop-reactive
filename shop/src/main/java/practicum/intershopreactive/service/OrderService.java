package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
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
    // TODO: add user logic
    private static final long USER_ID = 1;

    private final OrderR2dbcRepository orderRepository;
    private final ProductR2dbcRepository productRepository;
    private final CartR2dbcRepository cartRepository;
    private final OrderItemR2dbcRepository orderItemRepository;
    private final TransactionalOperator transactionalOperator;

    public Mono<Long> purchaseCart() {
        return cartRepository.findByUserId(USER_ID)
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new IllegalStateException("Cart is empty, cannot create order"));
                    }

                    List<Long> productIds = cartItems.stream()
                            .map(CartItem::getProductId)
                            .toList();

                    return productRepository.findAllById(productIds)
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
                                        .userId(USER_ID)
                                        .totalSum(totalSum)
                                        .build();

                                return orderRepository.save(order)
                                        .flatMap(savedOrder -> {
                                            orderItems.forEach(item -> item.setOrderId(savedOrder.getId()));

                                            Mono<Void> saveOrderItems = orderItemRepository.saveAll(orderItems).then();
                                            Mono<Void> deleteCartItems = cartRepository.deleteAllByUserId(USER_ID);

                                            return Mono.when(saveOrderItems, deleteCartItems)
                                                    .thenReturn(savedOrder.getId());
                                        });
                            });
                })
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
                    Mono<Product> productMono = productRepository.findById(orderItem.getProductId());
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
                                        productRepository.findById(orderItem.getProductId())
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

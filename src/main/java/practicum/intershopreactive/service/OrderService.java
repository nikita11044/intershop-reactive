package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import practicum.intershopreactive.dto.order.OrderDto;
import practicum.intershopreactive.dto.order.OrderItemDto;
import practicum.intershopreactive.entity.Order;
import practicum.intershopreactive.entity.OrderItem;
import practicum.intershopreactive.mapper.OrderMapper;
import practicum.intershopreactive.r2dbc.OrderItemR2dbcRepository;
import practicum.intershopreactive.r2dbc.OrderR2dbcRepository;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderR2dbcRepository orderRepository;
    private final ProductR2dbcRepository productRepository;
    private final OrderItemR2dbcRepository orderItemRepository;
    private final TransactionalOperator transactionalOperator;

    public Mono<Long> purchaseCart() {
        return productRepository.findByCountGreaterThan(0)
                .collectList()
                .flatMap(productsInCart -> {
                    if (productsInCart.isEmpty()) {
                        return Mono.error(new IllegalStateException("Cart is empty, cannot create order"));
                    }

                    Order order = new Order();
                    order.setCreatedAt(Instant.now());

                    // Calculate total sum
                    BigDecimal totalSum = productsInCart.stream()
                            .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getCount())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    order.setTotalSum(totalSum);

                    // Save order first (without items)
                    return orderRepository.save(order)
                            .flatMap(savedOrder -> {
                                // Create OrderItems with savedOrder id
                                List<OrderItem> orderItems = productsInCart.stream()
                                        .map(product -> OrderItem.builder()
                                                .orderId(savedOrder.getId())  // set orderId manually
                                                .productId(product.getId())
                                                .quantity(product.getCount())
                                                .priceAtPurchase(product.getPrice())
                                                .build())
                                        .toList();

                                // Save all order items
                                return orderItemRepository.saveAll(orderItems)
                                        .thenMany(
                                                // Reset product counts to 0
                                                Flux.fromIterable(productsInCart)
                                                        .flatMap(product -> {
                                                            product.setCount(0);
                                                            return productRepository.save(product);
                                                        })
                                        )
                                        .then(Mono.just(savedOrder.getId()));
                            });
                })
                .as(transactionalOperator::transactional);
    }


    public Flux<OrderDto> findAllWithItemsAndProducts() {
        return orderRepository.findAll()
                .flatMap(order ->
                        orderItemRepository.findByOrderId(order.getId())
                                .flatMap(orderItem ->
                                        productRepository.findById(orderItem.getProductId())
                                                .map(product -> {
                                                    return OrderItemDto.builder()
                                                            .id(orderItem.getId())
                                                            .productId(product.getId())
                                                            .title(product.getTitle())
                                                            .description(product.getDescription())
                                                            .imgPath(product.getImgPath())
                                                            .quantity(orderItem.getQuantity())
                                                            .priceAtPurchase(orderItem.getPriceAtPurchase())
                                                            .build();
                                                })
                                )
                                .collectList()
                                .map(orderItemDtos -> {
                                    return OrderDto.builder()
                                            .id(order.getId())
                                            // .totalSum(order.getTotalSum())
                                            .items(orderItemDtos)
                                            .build();
                                })
                );
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
                                        // .totalSum(order.getTotalSum())
                                        .items(orderItemDtos)
                                        .build())
                );
    }

}

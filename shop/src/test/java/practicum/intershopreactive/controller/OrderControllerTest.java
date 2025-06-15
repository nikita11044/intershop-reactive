package practicum.intershopreactive.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import practicum.intershopreactive.dto.order.OrderDto;
import practicum.intershopreactive.dto.order.OrderItemDto;
import practicum.intershopreactive.service.OrderService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(OrderController.class)
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void createOrder_shouldRedirectToOrderPageWithNewOrderParam() {
        Long orderId = 1L;
        when(orderService.purchaseCart())
                .thenReturn(Mono.just(orderId));

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/orders/1?newOrder=true");
    }

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void listOrders_shouldReturnOrdersViewWithModel() {
        OrderDto order = createOrder(1L, List.of(
                createOrderItem(new BigDecimal("999.99"), 1)
        ));

        when(orderService.findAllWithItemsAndProducts())
                .thenReturn(Flux.just(order));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("<!DOCTYPE html>");
                    assert body.contains("999.99");
                });
    }

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void getOrder_shouldReturnOrderViewWithDetails() {
        Long orderId = 1L;
        OrderDto order = createOrder(orderId, List.of(
                createOrderItem(new BigDecimal("999.99"), 2)
        ));

        when(orderService.getOrderById(orderId))
                .thenReturn(Mono.just(order));

        webTestClient.get()
                .uri("/orders/{id}?newOrder=true", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("<!DOCTYPE html>");
                    assert body.contains("Поздравляем! Успешная покупка!");
                    assert body.contains("2 шт.");
                    assert body.contains("1999.98 руб.");
                });
    }

    private OrderDto createOrder(Long id, List<OrderItemDto> items) {
        return OrderDto.builder()
                .id(id)
                .items(items)
                .build();
    }

    private OrderItemDto createOrderItem(BigDecimal price, long quantity) {
        return OrderItemDto.builder()
                .id(11L)
                .productId(1L)
                .quantity(quantity)
                .priceAtPurchase(price)
                .build();
    }
}

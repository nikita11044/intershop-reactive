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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import practicum.intershopreactive.dto.cart.CartDto;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.service.CartService;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(CartController.class)
@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    @Test
    void getCart_shouldRedirect_whenNotAuthenticated() {
        webTestClient.get()
                .uri("/cart")
                .exchange()
                .expectStatus()
                .is3xxRedirection();
    }

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void getCart_shouldReturnEmptyCartInitially() {
        when(cartService.getAllCartItems())
                .thenReturn(Mono.just(createEmptyCart()));

        webTestClient.get()
                .uri("/cart")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("<!DOCTYPE html>");
                    assert body.contains("Корзина товаров");
                    assert body.contains("Итого: 0 руб.");
                });
    }

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void modifyCartItem_shouldAddProductToCartAndCartShouldReflectIt() {
        when(cartService.addProduct(1L))
                .thenReturn(Mono.empty());

        var cart = createCartWithProduct(1L, "Laptop", new BigDecimal("1500.00"), 6);
        when(cartService.getAllCartItems())
                .thenReturn(Mono.just(cart));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("action", "PLUS");

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/cart/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get()
                .uri("/cart")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("<!DOCTYPE html>");
                    assert body.contains("Корзина товаров");
                    assert body.contains("Итого: 9000.00 руб.");
                });
    }

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void modifyCartItem_shouldIncrementAndDecrementCartCorrectly() {
        when(cartService.addProduct(1L))
                .thenReturn(Mono.empty());

        when(cartService.removeProduct(1L))
                .thenReturn(Mono.empty());

        var cartAfterFirstAdd = createCartWithProduct(1L, "Monitor", new BigDecimal("300.00"), 1);
        var cartAfterSecondAdd = createCartWithProduct(1L, "Monitor", new BigDecimal("300.00"), 2);
        var cartAfterDecrement = createCartWithProduct(1L, "Monitor", new BigDecimal("300.00"), 1);

        when(cartService.getAllCartItems())
                .thenReturn(Mono.just(cartAfterFirstAdd))
                .thenReturn(Mono.just(cartAfterSecondAdd))
                .thenReturn(Mono.just(cartAfterDecrement));

        MultiValueMap<String, String> plusAction = new LinkedMultiValueMap<>();
        plusAction.add("action", "PLUS");

        MultiValueMap<String, String> minusAction = new LinkedMultiValueMap<>();
        minusAction.add("action", "MINUS");

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/cart/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(plusAction)
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get()
                .uri("/cart")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("<!DOCTYPE html>");
                    assert body.contains("Корзина товаров");
                    assert body.contains("Monitor");
                    assert body.contains("300.00");
                });

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/cart/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(plusAction)
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get()
                .uri("/cart")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("<!DOCTYPE html>");
                    assert body.contains("Корзина товаров");
                    assert body.contains("Monitor");
                    assert body.contains("600.00");
                });

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/cart/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(minusAction)
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get()
                .uri("/cart")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("<!DOCTYPE html>");
                    assert body.contains("Корзина товаров");
                    assert body.contains("Monitor");
                    assert body.contains("300.00");
                });
    }

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void modifyCartItem_shouldDeleteItemFromCart() {
        when(cartService.addProduct(1L))
                .thenReturn(Mono.empty());

        when(cartService.deleteProduct(1L))
                .thenReturn(Mono.empty());

        when(cartService.getAllCartItems())
                .thenReturn(Mono.just(createEmptyCart()));

        MultiValueMap<String, String> plusAction = new LinkedMultiValueMap<>();
        plusAction.add("action", "PLUS");

        MultiValueMap<String, String> deleteAction = new LinkedMultiValueMap<>();
        deleteAction.add("action", "DELETE");

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/cart/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(plusAction)
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/cart/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(deleteAction)
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get()
                .uri("/cart")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("<!DOCTYPE html>");
                    assert body.contains("Корзина товаров");
                    assert body.contains("Итого: 0 руб.");
                });
    }

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void modifyCartItem_shouldReturn500ForInvalidAction() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("action", "INVALID");

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/cart/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    private ProductDto createProduct(Long id, String title, BigDecimal price, int count) {
        return ProductDto.builder()
                .id(id)
                .title(title)
                .description("Test description")
                .imgPath("test-image.jpg")
                .count(count)
                .price(price)
                .build();
    }

    private CartDto createCartWithProduct(Long id, String title, BigDecimal price, int count) {
        return CartDto.builder()
                .items(List.of(
                        createProduct(id, title, price, count)
                ))
                .total(price.multiply(BigDecimal.valueOf(count)))
                .empty(false)
                .available(true)
                .canBuy(true)
                .build();
    }

    private CartDto createEmptyCart() {
        return CartDto.builder()
                .items(Collections.emptyList())
                .total(BigDecimal.ZERO)
                .empty(true)
                .available(true)
                .canBuy(false)
                .build();
    }
}

package practicum.intershopreactive.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.service.CartService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@WebFluxTest(CartController.class)
@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    @Test
    void getCart_shouldReturnEmptyCartInitially() {
        when(cartService.getAllCartItems())
                .thenReturn(Mono.empty());

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
    void modifyCartItem_shouldAddProductToCartAndCartShouldReflectIt() {
        when(cartService.addProduct(1L))
                .thenReturn(Mono.empty());

        ProductDto product = createProduct(1L, "Laptop", new BigDecimal("1500.00"), 6);
        when(cartService.getAllCartItems())
                .thenReturn(Flux.just(product));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("action", "PLUS");

        webTestClient.post()
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
    void modifyCartItem_shouldIncrementAndDecrementCartCorrectly() {
        when(cartService.addProduct(1L))
                .thenReturn(Mono.empty());

        when(cartService.removeProduct(1L))
                .thenReturn(Mono.empty());

        ProductDto productAfterFirstAdd = createProduct(1L, "Monitor", new BigDecimal("300.00"), 1);
        ProductDto productAfterSecondAdd = createProduct(1L, "Monitor", new BigDecimal("300.00"), 2);
        ProductDto productAfterDecrement = createProduct(1L, "Monitor", new BigDecimal("300.00"), 1);

        when(cartService.getAllCartItems())
                .thenReturn(Mono.just(productAfterFirstAdd))
                .thenReturn(Mono.just(productAfterSecondAdd))
                .thenReturn(Mono.just(productAfterDecrement));

        MultiValueMap<String, String> plusAction = new LinkedMultiValueMap<>();
        plusAction.add("action", "PLUS");

        MultiValueMap<String, String> minusAction = new LinkedMultiValueMap<>();
        minusAction.add("action", "MINUS");

        webTestClient.post()
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

        webTestClient.post()
                .uri("/cart/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(plusAction)
                .exchange()
                .expectStatus().is3xxRedirection();

        // Verify cart after second increment (2 items × 300.00 = 600.00)
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

        webTestClient.post()
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
    void modifyCartItem_shouldDeleteItemFromCart() {
        when(cartService.addProduct(1L))
                .thenReturn(Mono.empty());

        when(cartService.deleteProduct(1L))
                .thenReturn(Mono.empty());

        when(cartService.getAllCartItems())
                .thenReturn(Mono.empty());

        MultiValueMap<String, String> plusAction = new LinkedMultiValueMap<>();
        plusAction.add("action", "PLUS");

        MultiValueMap<String, String> deleteAction = new LinkedMultiValueMap<>();
        deleteAction.add("action", "DELETE");

        webTestClient.post()
                .uri("/cart/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(plusAction)
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.post()
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
    void modifyCartItem_shouldReturn500ForInvalidAction() {
        // Given
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("action", "INVALID");

        // When & Then
        webTestClient.post()
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
}

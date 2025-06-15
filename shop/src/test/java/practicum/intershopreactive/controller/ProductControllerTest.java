package practicum.intershopreactive.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import practicum.intershopreactive.dto.PagingDto;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.dto.product.ProductPageDto;
import practicum.intershopreactive.service.CartService;
import practicum.intershopreactive.service.ProductService;
import practicum.intershopreactive.util.SortingType;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CartService cartService;

    @Test
    void modifyCart_shouldFail_whenNotAuthenticated() {
        webTestClient
                .post()
                .uri("/products/1/cart")
                .header("Referer", "/products/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("action=PLUS")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void listProducts_shouldReturnPageViewWithProductList() {
        var product = ProductDto
                .builder()
                .id(1L)
                .title("Test Product")
                .description("Description")
                .imgPath("image.jpg")
                .count(5)
                .price(new BigDecimal("99.99"))
                .build();

        var paging = PagingDto.builder()
                .pageNumber(1)
                .pageSize(10)
                .hasPrevious(false)
                .hasNext(false)
                .build();

        var productPage = ProductPageDto.builder()
                .items(List.of(product))
                .search("")
                .sort(SortingType.NO)
                .paging(paging)
                .build();

        when(productService.findProducts("", SortingType.NO, 10, 1))
                .thenReturn(Mono.just(productPage));

        webTestClient.get().uri("/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String html = result.getResponseBody();
                    assert html != null;
                    assert html.contains("Test Product");
                });
    }

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void getProduct_shouldReturnProductPage() {
        var product = ProductDto
                .builder()
                .id(1L)
                .title("Gadget")
                .description("Cool gadget")
                .imgPath("image.jpg")
                .count(10)
                .price(new BigDecimal("49.99"))
                .build();

        when(productService.getProductById(1L)).thenReturn(Mono.just(product));

        webTestClient.get().uri("/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String html = result.getResponseBody();
                    assert html != null;
                    assert html.contains("Gadget");
                });
    }

    @Test
    @WithMockUser(username = "John Doe", roles = {"CUSTOMER"})
    void modifyCart_shouldHandleValidPlusAction() {
        when(cartService.addProduct(1L)).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/products/1/cart")
                .header("Referer", "/products/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("action=PLUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/products/1");

        verify(cartService).addProduct(1L);
    }
}

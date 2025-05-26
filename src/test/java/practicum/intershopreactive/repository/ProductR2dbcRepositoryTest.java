package practicum.intershopreactive.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import practicum.intershopreactive.util.BaseContextTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ProductR2dbcRepositoryTest extends BaseContextTest {

    @Autowired
    private ProductR2dbcRepository productRepository;

    @BeforeEach
    void setUp() {
        dbHelper.clearAndResetDatabase().block();
        dbHelper.createMockProducts().block();
    }

    @Test
    void testFindProducts_WithSearchAndAlphaSort() {
        String search = "mouse";
        String searchPattern = "%" + search + "%";
        String sort = "ALPHA";
        int limit = 10;
        int offset = 0;

        Flux<Product> products = productRepository.findProducts(search, searchPattern, sort, limit, offset);

        StepVerifier.create(products)
                .expectNextMatches(product -> product.getDescription().toLowerCase().contains("mouse"))
                .expectComplete()
                .verify();
    }

    @Test
    void testFindProducts_WithoutSearchAndPriceSort() {
        String search = null;
        String searchPattern = null;
        String sort = "PRICE";
        int limit = 5;
        int offset = 0;

        Flux<Product> products = productRepository.findProducts(search, searchPattern, sort, limit, offset);

        StepVerifier.create(products)
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    void testCountProducts_WithSearch() {
        String search = "monitor";
        String searchPattern = "%" + search + "%";

        Mono<Long> count = productRepository.countProducts(search, searchPattern);

        StepVerifier.create(count)
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void testCountProducts_WithoutSearch() {
        String search = null;
        String searchPattern = null;

        Mono<Long> count = productRepository.countProducts(search, searchPattern);

        StepVerifier.create(count)
                .expectNext(5L)
                .verifyComplete();
    }
}

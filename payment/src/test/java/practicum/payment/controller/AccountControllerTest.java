package practicum.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import practicum.payment.util.BaseWebTest;
import practicum.payment.util.TestDatabaseHelper;

import java.math.BigDecimal;

public class AccountControllerTest extends BaseWebTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void setUp() {
        TestDatabaseHelper dbHelper = new TestDatabaseHelper(databaseClient);
        dbHelper.clearAndResetAccounts()
                .then(dbHelper.createMockAccount(1L, new BigDecimal("99.99")))
                .block();
    }

    @Test
    void getBalance_withoutAuth_shouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/balance/{userId}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }


    @Test
    @WithMockUser
    void getBalance_shouldReturnBalance() {
        webTestClient.get()
                .uri("/balance/{userId}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo("1")
                .jsonPath("$.balance").isEqualTo(99.99);
    }

    @Test
    @WithMockUser
    void getBalance_shouldReturn404_whenAccountMissing() {
        webTestClient.get()
                .uri("/balance/{userId}", 2)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }
}

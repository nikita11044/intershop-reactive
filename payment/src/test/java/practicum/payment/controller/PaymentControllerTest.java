package practicum.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import practicum.payment.model.PaymentRequest;
import practicum.payment.util.BaseWebTest;
import practicum.payment.util.TestDatabaseHelper;
import org.springframework.r2dbc.core.DatabaseClient;

import java.math.BigDecimal;

public class PaymentControllerTest extends BaseWebTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void setup() {
        TestDatabaseHelper dbHelper = new TestDatabaseHelper(databaseClient);
        dbHelper.clearAndResetAccounts()
                .then(dbHelper.createMockAccount(1L, new BigDecimal("100.00")))
                .block();
    }

    @Test
    void processPayment_shouldSucceed_whenSufficientBalance() {
        PaymentRequest request = new PaymentRequest()
                .userId(1L)
                .amount(new BigDecimal("40.00"));

        webTestClient.post()
                .uri("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    void processPayment_shouldFail_whenInsufficientBalance() {
        PaymentRequest request = new PaymentRequest()
                .userId(1L)
                .amount(new BigDecimal("1000.00"));

        webTestClient.post()
                .uri("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }
}

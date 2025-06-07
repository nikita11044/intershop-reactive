package practicum.payment.util;

import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

public class TestDatabaseHelper {

    private final DatabaseClient databaseClient;

    public TestDatabaseHelper(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Void> clearAndResetAccounts() {
        return databaseClient.sql("DELETE FROM accounts").then()
                .then(databaseClient.sql("ALTER SEQUENCE accounts_id_seq RESTART WITH 1").then());
    }

    public Mono<Void> createMockAccounts() {
        List<Mono<Void>> inserts = List.of(
                createMockAccount(1L, new BigDecimal("99.99")),
                createMockAccount(2L, new BigDecimal("0.00")),
                createMockAccount(3L, new BigDecimal("1000.00"))
        );

        return Mono.when(inserts);
    }

    public Mono<Void> createMockAccount(Long userId, BigDecimal balance) {
        String sql = "INSERT INTO accounts (user_id, balance) VALUES (:userId, :balance)";
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .bind("balance", balance)
                .then();
    }
}

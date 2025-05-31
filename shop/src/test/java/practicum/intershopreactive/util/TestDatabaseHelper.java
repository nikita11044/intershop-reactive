package practicum.intershopreactive.util;

import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public class TestDatabaseHelper {

    private final DatabaseClient databaseClient;

    public TestDatabaseHelper(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Void> clearAndResetDatabase() {
        return databaseClient.sql("DELETE FROM order_items").then()
                .then(databaseClient.sql("DELETE FROM orders").then())
                .then(databaseClient.sql("DELETE FROM products").then())
                .then(databaseClient.sql("ALTER SEQUENCE order_items_id_seq RESTART WITH 1").then())
                .then(databaseClient.sql("ALTER SEQUENCE orders_id_seq RESTART WITH 1").then())
                .then(databaseClient.sql("ALTER SEQUENCE products_id_seq RESTART WITH 1").then());
    }

    public Mono<Void> createMockProducts() {
        return createMockProduct("Apple MacBook Pro", "16-inch, M1 Pro chip", new BigDecimal("2499.99"), 10, "macbook.jpg")
                .then(createMockProduct("Logitech MX Master 3", "Wireless mouse for productivity", new BigDecimal("99.99"), 25, "mouse.jpg"))
                .then(createMockProduct("Dell Ultrasharp Monitor", "27-inch 4K IPS monitor", new BigDecimal("599.99"), 7, "monitor.jpg"))
                .then(createMockProduct("Mechanical Keyboard", "RGB backlit, blue switches", new BigDecimal("129.99"), 15, "keyboard.jpg"))
                .then(createMockProduct("USB-C Hub", "Multiport adapter with HDMI and USB", new BigDecimal("49.99"), 50, "hub.jpg"));
    }

    public Mono<Void> createMockProduct(String title, String description, BigDecimal price, int count, String imagePath) {
        String sql = "INSERT INTO products (title, description, price, count, img_path) VALUES (:title, :description, :price, :count, :imagePath)";
        return databaseClient.sql(sql)
                .bind("title", title)
                .bind("description", description)
                .bind("price", price)
                .bind("count", count)
                .bind("imagePath", imagePath)
                .then();
    }
}

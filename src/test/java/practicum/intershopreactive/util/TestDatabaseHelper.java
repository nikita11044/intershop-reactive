package practicum.intershopreactive.util;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

public class TestDatabaseHelper {
    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clearAndResetDatabase() {
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM products");

        jdbcTemplate.execute("ALTER SEQUENCE order_items_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE orders_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE products_id_seq RESTART WITH 1");
    }

    public void createMockProduct() {
        createMockProduct("Sample Product", "Sample Description", new BigDecimal("19.99"), 5, "sample.jpg");
    }

    public void createMockProducts() {
        createMockProduct("Apple MacBook Pro", "16-inch, M1 Pro chip", new BigDecimal("2499.99"), 10, "macbook.jpg");
        createMockProduct("Logitech MX Master 3", "Wireless mouse for productivity", new BigDecimal("99.99"), 25, "mouse.jpg");
        createMockProduct("Dell Ultrasharp Monitor", "27-inch 4K IPS monitor", new BigDecimal("599.99"), 7, "monitor.jpg");
        createMockProduct("Mechanical Keyboard", "RGB backlit, blue switches", new BigDecimal("129.99"), 15, "keyboard.jpg");
        createMockProduct("USB-C Hub", "Multiport adapter with HDMI and USB", new BigDecimal("49.99"), 50, "hub.jpg");
    }

    public void createMockProduct(String title, String description, BigDecimal price, int count, String imagePath) {
        String sql = "INSERT INTO products (title, description, price, count, img_path) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, title, description, price, count, imagePath);
    }
    public void createMockOrder(Long id, Instant createdAt, BigDecimal totalSum) {
        String sql = "INSERT INTO orders (id, created_at, total_sum) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, id, Timestamp.from(createdAt), totalSum);
    }
}

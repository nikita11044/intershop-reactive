package practicum.intershopreactive.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("order_items")
public class OrderItem {

    @Id
    private Long id;

    @NotNull
    @Column("order_id")
    private Long orderId;

    @NotNull
    @Column("product_id")
    private Long productId;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @Column("price_at_purchase")
    private BigDecimal priceAtPurchase;
}

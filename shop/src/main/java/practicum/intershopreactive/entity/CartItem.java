package practicum.intershopreactive.entity;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("cart_items")
public class CartItem {

    @Id
    private Long id;

    @NotNull
    @Column("user_id")
    private Long userId;

    @NotNull
    @Column("product_id")
    private Long productId;

    @NotNull
    @Column("count")
    private Long count;
}

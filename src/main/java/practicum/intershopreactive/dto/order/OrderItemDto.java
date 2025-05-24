package practicum.intershopreactive.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private Long id;
    private Long productId;
    private String title;
    private String description;
    private String imgPath;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
}

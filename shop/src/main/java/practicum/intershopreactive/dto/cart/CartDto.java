package practicum.intershopreactive.dto.cart;

import lombok.Builder;
import lombok.Data;
import practicum.intershopreactive.dto.product.ProductDto;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartDto {
    private List<ProductDto> items;
    private boolean empty;
    private BigDecimal total;
    private boolean canBuy;
    private boolean available;
}

package practicum.intershopreactive.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import practicum.intershopreactive.dto.PagingDto;
import practicum.intershopreactive.util.SortingType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPageDto {
    private List<ProductDto> items;
    private String search;
    private SortingType sort;
    private PagingDto paging;
}

package practicum.intershopreactive.mapper;

import org.mapstruct.Mapper;
import practicum.intershopreactive.dto.product.CreateProductDto;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product toEntity(CreateProductDto createProductDto);

    ProductDto toDto(Product product);
}


package practicum.intershopreactive.dto.product;

import lombok.Data;
import org.springframework.http.codec.multipart.FilePart;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductFormDto {
    private List<CreateProductDto> products = new ArrayList<>();
}


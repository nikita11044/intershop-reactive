package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import practicum.intershopreactive.dto.PagingDto;
import practicum.intershopreactive.dto.product.CreateProductDto;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.dto.product.ProductPageDto;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import practicum.intershopreactive.mapper.ProductMapper;
import practicum.intershopreactive.util.SortingType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductR2dbcRepository productRepository;
    private final ProductMapper productMapper;
    private final FileService fileService;

    public Mono<ProductPageDto> findProducts(String search, SortingType sort, int pageSize, int pageNumber) {
        int offset = (pageNumber - 1) * pageSize;
        String searchPattern = "%" + search + "%";

        Flux<ProductDto> productsFlux = productRepository.findProducts(search, searchPattern, sort.name(), pageSize, offset)
                .map(productMapper::toDto);

        Mono<Long> countMono = productRepository.countProducts(search, searchPattern);

        return Mono.zip(productsFlux.collectList(), countMono)
                .map(tuple -> {
                    List<ProductDto> products = tuple.getT1();
                    Long total = tuple.getT2();

                    PagingDto paging = PagingDto.builder()
                            .pageNumber(pageNumber)
                            .pageSize(pageSize)
                            .hasPrevious(pageNumber > 1)
                            .hasNext((long) pageNumber * pageSize < total)
                            .build();

                    return ProductPageDto.builder()
                            .items(arrangeInRows(products))
                            .search(search)
                            .sort(sort)
                            .paging(paging)
                            .build();
                });
    }

    public Mono<ProductDto> getProductById(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found with id: " + id)))
                .map(productMapper::toDto);
    }

    public Flux<Product> addProductsWithFiles(List<CreateProductDto> productCreateDtos, List<FilePart> files) {
        Map<Integer, FilePart> fileMap = IntStream.range(0, files.size())
                .boxed()
                .collect(Collectors.toMap(i -> i, files::get));

        return Flux.range(0, productCreateDtos.size())
                .filter(i -> {
                    CreateProductDto dto = productCreateDtos.get(i);
                    return dto.getTitle() != null && !dto.getTitle().isBlank() && dto.getPrice() > 0;
                })
                .flatMap(i -> {
                    CreateProductDto dto = productCreateDtos.get(i);
                    FilePart filePart = fileMap.get(i);

                    if (dto.getCount() == null) {
                        dto.setCount(0);
                    }

                    Mono<String> imgPathMono;
                    if (filePart != null) {
                        imgPathMono = fileService.uploadFile(filePart);
                    } else {
                        imgPathMono = Mono.justOrEmpty(null);
                    }

                    return imgPathMono.map(imgPath -> {
                        Product product = productMapper.toEntity(dto);
                        product.setImgPath(imgPath);
                        return product;
                    });
                })
                .flatMap(productRepository::save);
    }



    private List<List<ProductDto>> arrangeInRows(List<ProductDto> products) {
        return java.util.stream.IntStream.range(0, (products.size() + 2) / 3)
                .mapToObj(i -> products.subList(i * 3, Math.min((i + 1) * 3, products.size())))
                .toList();
    }
}

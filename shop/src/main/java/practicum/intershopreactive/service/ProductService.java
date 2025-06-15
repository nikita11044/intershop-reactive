package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import practicum.intershopreactive.dto.PagingDto;
import practicum.intershopreactive.dto.product.CreateProductDto;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.dto.product.ProductPageDto;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.mapper.ProductMapper;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import practicum.intershopreactive.service.cache.ProductCacheService;
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
    private final CartR2dbcRepository cartRepository;
    private final ProductMapper productMapper;
    private final FileService fileService;
    private final ProductCacheService productCacheService;
    private final UserService userService;

    public Mono<ProductPageDto> findProducts(String search, SortingType sort, int pageSize, int pageNumber) {
        int offset = (pageNumber - 1) * pageSize;
        String searchPattern = "%" + search + "%";

        Flux<Product> productsFlux = productCacheService.findProducts(search, searchPattern, sort.name(), pageSize, offset);

        return userService.getCurrentUserId()
                .flatMap(userId -> cartRepository.findByUserId(userId).collectMap(CartItem::getProductId, CartItem::getCount)
                        .flatMapMany(cartItemsMap -> productsFlux.collectList().flatMapMany(products ->
                                Flux.fromIterable(products)
                                        .map(product -> {
                                            int count = cartItemsMap.getOrDefault(product.getId(), 0L).intValue();
                                            return ProductDto.builder()
                                                    .id(product.getId())
                                                    .title(product.getTitle())
                                                    .description(product.getDescription())
                                                    .imgPath(product.getImgPath())
                                                    .price(product.getPrice())
                                                    .count(count)
                                                    .build();
                                        })
                        ))
                        .collectList()
                        .zipWith(productRepository.countProducts(search, searchPattern))
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
                                    .items(products)
                                    .search(search)
                                    .sort(sort)
                                    .paging(paging)
                                    .build();
                        })
                );
    }

    public Mono<ProductDto> getProductById(Long id) {
        Mono<Product> productMono = productCacheService.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found with id: " + id)));

        return userService.getCurrentUserId()
                .flatMap(userId -> {
                    Mono<Long> countMono = cartRepository.findByProductIdAndUserId(id, userId)
                            .map(CartItem::getCount)
                            .defaultIfEmpty(0L);

                    return Mono.zip(productMono, countMono)
                            .map(tuple -> {
                                Product product = tuple.getT1();
                                Long count = tuple.getT2();

                                return ProductDto.builder()
                                        .id(product.getId())
                                        .title(product.getTitle())
                                        .description(product.getDescription())
                                        .imgPath(product.getImgPath())
                                        .price(product.getPrice())
                                        .count(count.intValue())
                                        .build();
                            });
                });
    }

    @Transactional
    public Flux<Product> addProductsWithFiles(List<CreateProductDto> productCreateDtos, List<FilePart> files) {
        Map<Integer, FilePart> fileMap = IntStream.range(0, files.size())
                .boxed()
                .collect(Collectors.toMap(i -> i, files::get));

        return userService.getCurrentUserId()
                .flatMapMany(userId -> Flux.range(0, productCreateDtos.size())
                        .flatMap(i -> {
                            CreateProductDto dto = productCreateDtos.get(i);
                            FilePart filePart = fileMap.get(i);

                            if (
                                    dto.getTitle() == null
                                            || dto.getTitle().isBlank()
                                            || dto.getPrice() <= 0
                                            || dto.getCount() == null
                                            || dto.getCount() <= 0
                            ) {
                                return Mono.empty();
                            }

                            Mono<String> imgPathMono = (filePart != null)
                                    ? fileService.uploadFile(filePart)
                                    : Mono.justOrEmpty(null);

                            return imgPathMono
                                    .map(imgPath -> {
                                        Product product = productMapper.toEntity(dto);
                                        product.setImgPath(imgPath);
                                        return product;
                                    })
                                    .flatMap(productRepository::save)
                                    .flatMap(savedProduct -> {
                                        CartItem cartItem = CartItem.builder()
                                                .userId(userId)
                                                .productId(savedProduct.getId())
                                                .count(dto.getCount().longValue())
                                                .build();

                                        return cartRepository.save(cartItem)
                                                .thenReturn(savedProduct);
                                    });
                        })
                        .collectList()
                        .flatMapMany(products -> productCacheService.evictProductsCache()
                                .thenMany(Flux.fromIterable(products))
                        )
                );
    }
}

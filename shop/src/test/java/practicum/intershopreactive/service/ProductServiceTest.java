
package practicum.intershopreactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.multipart.FilePart;
import practicum.intershopreactive.dto.product.CreateProductDto;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.mapper.ProductMapper;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import practicum.intershopreactive.service.cache.ProductCacheService;
import practicum.intershopreactive.util.SortingType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductR2dbcRepository productRepository;
    @Mock private CartR2dbcRepository cartRepository;
    @Mock private ProductMapper productMapper;
    @Mock private FileService fileService;
    @Mock private ProductCacheService productCacheService;

    @InjectMocks private ProductService productService;

    private Product testProduct;
    private CreateProductDto testCreateProductDto;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setTitle("Sample Product");
        testProduct.setDescription("Sample Description");
        testProduct.setPrice(new BigDecimal("100.0"));
        testProduct.setImgPath("sample.jpg");

        testCreateProductDto = CreateProductDto.builder()
                .title("Sample Product").description("Sample Description")
                .price(100.0).count(10).build();
    }

    @Test
    void testGetProductById_found() {
        Long productId = 1L;
        when(productCacheService.findById(productId)).thenReturn(Mono.just(testProduct));
        when(cartRepository.findByProductIdAndUserId(eq(productId), anyLong()))
                .thenReturn(Mono.just(CartItem.builder().count(5L).build()));

        StepVerifier.create(productService.getProductById(productId))
                .expectNextMatches(dto -> dto.getId().equals(productId) && dto.getCount() == 5)
                .verifyComplete();

        verify(productCacheService).findById(productId);
    }

    @Test
    void testGetProductById_notFound() {
        Long productId = 1L;
        when(productCacheService.findById(productId)).thenReturn(Mono.empty());
        when(cartRepository.findByProductIdAndUserId(eq(productId), anyLong()))
                .thenReturn(Mono.empty());

        StepVerifier.create(productService.getProductById(productId))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Product not found with id"))
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void testFindProducts_withSearch() {
        String search = "Sample";
        SortingType sort = SortingType.NO;
        int pageSize = 10, pageNumber = 1;
        String searchPattern = "%" + search + "%";
        List<Product> products = List.of(testProduct);

        when(productCacheService.findProducts(eq(search), eq(searchPattern), eq(sort.name()), eq(pageSize), eq(0)))
                .thenReturn(Flux.fromIterable(products));
        when(cartRepository.findByUserId(anyLong())).thenReturn(Flux.empty());
        when(productRepository.countProducts(eq(search), eq(searchPattern))).thenReturn(Mono.just(1L));

        StepVerifier.create(productService.findProducts(search, sort, pageSize, pageNumber))
                .expectNextMatches(page -> page.getItems().size() == 1 &&
                        page.getItems().get(0).getTitle().equals("Sample Product") &&
                        page.getPaging().getPageNumber() == 1)
                .verifyComplete();
    }

    @Test
    void testAddProductsWithFiles_validInput() {
        List<CreateProductDto> dtos = List.of(testCreateProductDto);
        FilePart filePart = mock(FilePart.class);
        List<FilePart> files = List.of(filePart);

        Product newProduct = new Product();
        newProduct.setTitle(testCreateProductDto.getTitle());
        newProduct.setDescription(testCreateProductDto.getDescription());
        newProduct.setPrice(new BigDecimal("100.0"));

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setImgPath("image.jpg");

        when(fileService.uploadFile(filePart)).thenReturn(Mono.just("image.jpg"));
        when(productMapper.toEntity(any())).thenReturn(newProduct);
        when(productRepository.save(any())).thenReturn(Mono.just(savedProduct));
        when(cartRepository.save(any())).thenReturn(Mono.just(new CartItem()));
        when(productCacheService.evictProductsCache()).thenReturn(Mono.empty());

        StepVerifier.create(productService.addProductsWithFiles(dtos, files))
                .expectNext(savedProduct)
                .verifyComplete();
    }
}

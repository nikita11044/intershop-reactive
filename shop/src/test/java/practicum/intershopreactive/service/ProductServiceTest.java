package practicum.intershopreactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.multipart.FilePart;
import practicum.intershopreactive.dto.PagingDto;
import practicum.intershopreactive.dto.product.CreateProductDto;
import practicum.intershopreactive.dto.product.ProductDto;
import practicum.intershopreactive.dto.product.ProductPageDto;
import practicum.intershopreactive.entity.CartItem;
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.mapper.ProductMapper;
import practicum.intershopreactive.r2dbc.CartR2dbcRepository;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import practicum.intershopreactive.util.SortingType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductR2dbcRepository productRepository;

    @Mock
    private CartR2dbcRepository cartRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private FileService fileService;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductDto testProductDto;
    private CreateProductDto testCreateProductDto;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setTitle("Sample Product");
        testProduct.setDescription("Sample Description");
        testProduct.setPrice(new BigDecimal("100.0"));
        testProduct.setImgPath("sample.jpg");

        testProductDto = ProductDto.builder()
                .id(1L)
                .title("Sample Product")
                .description("Sample Description")
                .price(new BigDecimal("100.0"))
                .count(10)
                .imgPath("sample.jpg")
                .build();

        testCreateProductDto = CreateProductDto.builder()
                .title("Sample Product")
                .description("Sample Description")
                .price(100.0)
                .count(10)
                .build();
    }

    @Test
    void testGetProductById_found() {
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Mono.just(testProduct));
        when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

        StepVerifier.create(productService.getProductById(productId))
                .expectNext(testProductDto)
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(productRepository).findById(productId);
        verify(productMapper).toDto(testProduct);
    }

    @Test
    void testGetProductById_notFound() {
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Mono.empty());

        StepVerifier.create(productService.getProductById(productId))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Product not found with id: " + productId))
                .verify(Duration.ofSeconds(5));

        verify(productRepository).findById(productId);
        verify(productMapper, never()).toDto(any());
    }

    @Test
    void testAddProductsWithFiles_validInput() {
        List<CreateProductDto> dtos = List.of(testCreateProductDto);
        FilePart mockFile = mock(FilePart.class);
        List<FilePart> files = List.of(mockFile);

        Product entity = new Product();
        entity.setTitle("Sample Product");
        entity.setDescription("Sample Description");
        entity.setPrice(new BigDecimal("100.0"));

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setTitle("Sample Product");
        savedProduct.setImgPath("imagePath");

        when(fileService.uploadFile(mockFile)).thenReturn(Mono.just("imagePath"));
        when(productMapper.toEntity(testCreateProductDto)).thenReturn(entity);
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(savedProduct));
        when(cartRepository.save(any(CartItem.class))).thenReturn(Mono.just(new CartItem()));

        StepVerifier.create(productService.addProductsWithFiles(dtos, files))
                .expectNext(savedProduct)
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(fileService).uploadFile(mockFile);
        verify(productMapper).toEntity(testCreateProductDto);
        verify(productRepository).save(any(Product.class));
        verify(cartRepository).save(any(CartItem.class));
    }

    @Test
    void testAddProductsWithFiles_invalidInput() {
        CreateProductDto invalidDto = CreateProductDto.builder()
                .title("")
                .price(0.0)
                .count(0)
                .build();
        List<CreateProductDto> dtos = List.of(invalidDto);
        List<FilePart> files = List.of();

        StepVerifier.create(productService.addProductsWithFiles(dtos, files))
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(fileService, never()).uploadFile(any());
        verify(productMapper, never()).toEntity(any());
        verify(productRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testFindProducts_withSearch() {
        String search = "Sample";
        SortingType sort = SortingType.NO;
        int pageSize = 10;
        int pageNumber = 1;
        int offset = 0;
        String searchPattern = "%Sample%";

        List<Product> products = List.of(testProduct);
        long totalCount = 1L;

        when(productRepository.findProducts(search, searchPattern, sort.name(), pageSize, offset))
                .thenReturn(Flux.fromIterable(products));
        when(cartRepository.findByUserId(anyLong())).thenReturn(Flux.empty());
        when(productRepository.countProducts(search, searchPattern)).thenReturn(Mono.just(totalCount));

        StepVerifier.create(productService.findProducts(search, sort, pageSize, pageNumber))
                .expectNextMatches(result -> {
                    List<ProductDto> items = result.getItems();
                    return items.size() == 1 &&
                            items.get(0).getTitle().equals("Sample Product") &&
                            result.getSearch().equals(search) &&
                            result.getSort().equals(sort);
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(productRepository).findProducts(search, searchPattern, sort.name(), pageSize, offset);
        verify(cartRepository).findByUserId(anyLong());
        verify(productRepository).countProducts(search, searchPattern);
    }

    @Test
    void testFindProducts_withPagination() {
        String search = "";
        SortingType sort = SortingType.NO;
        int pageSize = 2;
        int pageNumber = 2;
        int offset = 2;
        String searchPattern = "%%";
        long totalCount = 5L;

        when(productRepository.findProducts(search, searchPattern, sort.name(), pageSize, offset))
                .thenReturn(Flux.fromIterable(List.of(testProduct)));
        when(cartRepository.findByUserId(anyLong())).thenReturn(Flux.empty());
        when(productRepository.countProducts(search, searchPattern)).thenReturn(Mono.just(totalCount));

        StepVerifier.create(productService.findProducts(search, sort, pageSize, pageNumber))
                .expectNextMatches(result -> {
                    PagingDto paging = result.getPaging();
                    return paging.getPageNumber() == pageNumber &&
                            paging.getPageSize() == pageSize &&
                            paging.isHasPrevious() &&
                            paging.isHasNext();
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(productRepository).findProducts(search, searchPattern, sort.name(), pageSize, offset);
        verify(cartRepository).findByUserId(anyLong());
        verify(productRepository).countProducts(search, searchPattern);
    }

    @Test
    void testAddProductsWithFiles_fileUploadError() {
        List<CreateProductDto> dtos = List.of(testCreateProductDto);
        FilePart file = mock(FilePart.class);
        List<FilePart> files = List.of(file);

        when(fileService.uploadFile(file)).thenReturn(Mono.error(new RuntimeException("Upload failed")));

        StepVerifier.create(productService.addProductsWithFiles(dtos, files))
                .expectError(RuntimeException.class)
                .verify(Duration.ofSeconds(5));

        verify(fileService).uploadFile(file);
        verify(productRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
    }
}

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
import practicum.intershopreactive.entity.Product;
import practicum.intershopreactive.mapper.ProductMapper;
import practicum.intershopreactive.r2dbc.ProductR2dbcRepository;
import practicum.intershopreactive.util.SortingType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductR2dbcRepository productRepository;

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
        testProduct.setCount(10);
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
    void testAddProducts_productsCreated() {
        List<CreateProductDto> dtos = List.of(testCreateProductDto);
        FilePart mockFile = mock(FilePart.class);
        List<FilePart> files = List.of(mockFile);

        Product entity = new Product();
        entity.setTitle("Sample Product");
        entity.setDescription("Sample Description");
        entity.setPrice(new BigDecimal("100.0"));
        entity.setCount(10);

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setTitle("Sample Product");
        savedProduct.setImgPath("imagePath");

        when(fileService.uploadFile(mockFile)).thenReturn(Mono.just("imagePath"));
        when(productMapper.toEntity(testCreateProductDto)).thenReturn(entity);
        when(productRepository.save(argThat(product ->
                "Sample Product".equals(product.getTitle()) && "imagePath".equals(product.getImgPath()))))
                .thenReturn(Mono.just(savedProduct));

        StepVerifier.create(productService.addProductsWithFiles(dtos, files))
                .expectNext(savedProduct)
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(fileService).uploadFile(mockFile);
        verify(productMapper).toEntity(testCreateProductDto);
        verify(productRepository).save(any(Product.class));
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
        when(productMapper.toDto(testProduct)).thenReturn(testProductDto);
        when(productRepository.countProducts(search, searchPattern))
                .thenReturn(Mono.just(totalCount));

        StepVerifier.create(productService.findProducts(search, sort, pageSize, pageNumber))
                .expectNextMatches(result -> {
                    boolean itemsMatch = result.getItems().size() == 1 &&
                            result.getItems().get(0).equals(testProductDto);
                    boolean searchMatch = result.getSearch().equals(search);
                    boolean sortMatch = result.getSort().equals(sort);
                    boolean pagingMatch = result.getPaging().getPageNumber() == pageNumber &&
                            result.getPaging().getPageSize() == pageSize &&
                            !result.getPaging().isHasPrevious() &&
                            !result.getPaging().isHasNext();
                    return itemsMatch && searchMatch && sortMatch && pagingMatch;
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(productRepository).findProducts(search, searchPattern, sort.name(), pageSize, offset);
        verify(productRepository).countProducts(search, searchPattern);
        verify(productMapper).toDto(testProduct);
    }

    @Test
    void testFindProducts_withoutSearch() {
        String search = "";
        SortingType sort = SortingType.NO;
        int pageSize = 10;
        int pageNumber = 1;
        int offset = 0;
        String searchPattern = "%%";

        List<Product> products = List.of(testProduct);
        long totalCount = 1L;

        when(productRepository.findProducts(search, searchPattern, sort.name(), pageSize, offset))
                .thenReturn(Flux.fromIterable(products));
        when(productMapper.toDto(testProduct)).thenReturn(testProductDto);
        when(productRepository.countProducts(search, searchPattern))
                .thenReturn(Mono.just(totalCount));

        StepVerifier.create(productService.findProducts(search, sort, pageSize, pageNumber))
                .expectNextMatches(result -> {
                    boolean itemsMatch = result.getItems().size() == 1 &&
                            result.getItems().get(0).equals(testProductDto);
                    boolean searchMatch = result.getSearch().equals(search);
                    boolean sortMatch = result.getSort().equals(sort);
                    return itemsMatch && searchMatch && sortMatch;
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(productRepository).findProducts(search, searchPattern, sort.name(), pageSize, offset);
        verify(productRepository).countProducts(search, searchPattern);
        verify(productMapper).toDto(testProduct);
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
        when(productMapper.toDto(testProduct)).thenReturn(testProductDto);
        when(productRepository.countProducts(search, searchPattern))
                .thenReturn(Mono.just(totalCount));

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
    }

    @Test
    void testFindProducts_returnsFlatListOfProducts() {
        Product product2 = new Product();
        product2.setId(2L);
        product2.setTitle("Product 2");

        Product product3 = new Product();
        product3.setId(3L);
        product3.setTitle("Product 3");

        Product product4 = new Product();
        product4.setId(4L);
        product4.setTitle("Product 4");

        ProductDto productDto2 = ProductDto.builder().id(2L).title("Product 2").build();
        ProductDto productDto3 = ProductDto.builder().id(3L).title("Product 3").build();
        ProductDto productDto4 = ProductDto.builder().id(4L).title("Product 4").build();

        List<Product> products = List.of(testProduct, product2, product3, product4);

        when(productRepository.findProducts(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Flux.fromIterable(products));
        when(productMapper.toDto(testProduct)).thenReturn(testProductDto);
        when(productMapper.toDto(product2)).thenReturn(productDto2);
        when(productMapper.toDto(product3)).thenReturn(productDto3);
        when(productMapper.toDto(product4)).thenReturn(productDto4);
        when(productRepository.countProducts(anyString(), anyString()))
                .thenReturn(Mono.just(4L));

        StepVerifier.create(productService.findProducts("", SortingType.NO, 10, 1))
                .expectNextMatches(result -> {
                    List<ProductDto> items = result.getItems();
                    return items.size() == 4 && // Now we expect a flat list of 4 products
                            items.contains(testProductDto) &&
                            items.contains(productDto2) &&
                            items.contains(productDto3) &&
                            items.contains(productDto4);
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void testAddProducts_handleFileUploadError() {
        List<CreateProductDto> dtos = List.of(testCreateProductDto);
        FilePart file = mock(FilePart.class);
        List<FilePart> files = List.of(file);

        when(fileService.uploadFile(file))
                .thenReturn(Mono.error(new RuntimeException("Upload failed")));

        StepVerifier.create(productService.addProductsWithFiles(dtos, files))
                .expectError(RuntimeException.class)
                .verify(Duration.ofSeconds(5));

        verify(fileService).uploadFile(file);
        verify(productRepository, never()).save(any());
    }
}

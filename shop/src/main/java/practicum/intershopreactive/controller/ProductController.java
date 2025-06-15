package practicum.intershopreactive.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import practicum.intershopreactive.dto.cart.CartActionFormDto;
import practicum.intershopreactive.dto.product.CreateProductDto;
import practicum.intershopreactive.dto.product.ProductFormDto;
import practicum.intershopreactive.service.CartService;
import practicum.intershopreactive.service.ProductService;
import practicum.intershopreactive.util.ActionType;
import practicum.intershopreactive.util.SecurityHelper;
import practicum.intershopreactive.util.SortingType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CartService cartService;

    @GetMapping
    public Mono<String> listProducts(
            Authentication authentication,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NO") SortingType sort,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNumber,
            Model model) {

        return productService.findProducts(search, sort, pageSize, pageNumber)
                .flatMap(page -> {
                    model.addAttribute("items", page.getItems());
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort.name());
                    model.addAttribute("paging", page.getPaging());
                    model.addAttribute("role", SecurityHelper.getRole(authentication));
                    return Mono.just("products");
                });
    }

    @GetMapping("/new")
    public Mono<String> showProductForm(Authentication authentication, Model model) {
        ProductFormDto productForm = new ProductFormDto();
        productForm.setProducts(List.of(new CreateProductDto(), new CreateProductDto(), new CreateProductDto()));
        model.addAttribute("productForm", productForm);
        model.addAttribute("role", SecurityHelper.getRole(authentication));
        return Mono.just("add-products");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> addProducts(
            @ModelAttribute ProductFormDto productFormDto,
            @RequestPart("files") Flux<FilePart> filesFlux) {

        return filesFlux.collectList()
                .flatMap(files ->
                        productService.addProductsWithFiles(productFormDto.getProducts(), files)
                                .then()
                )
                .then(Mono.just("redirect:/products"));
    }

    @GetMapping("/{id}")
    public Mono<String> getProduct(
            Authentication authentication,
            @PathVariable Long id, Model model
    ) {
        return productService.getProductById(id)
                .doOnNext(product -> {
                    model.addAttribute("item", product);
                    model.addAttribute("role", SecurityHelper.getRole(authentication));
                })
                .thenReturn("product");
    }

    @PostMapping("/{id}/cart")
    public Mono<String> modifyCartFromProduct(
            @PathVariable Long id,
            @ModelAttribute CartActionFormDto dto,
            @RequestHeader(value = "Referer", required = false) String referer
    ) {
        ActionType actionType;

        actionType = ActionType.valueOf(dto.getAction().toUpperCase());

        Mono<Void> actionMono;
        switch (actionType) {
            case PLUS -> actionMono = cartService.addProduct(id);
            case MINUS -> actionMono = cartService.removeProduct(id);
            case DELETE -> actionMono = cartService.deleteProduct(id);
            default -> {
                return Mono.error(new IllegalArgumentException("Unknown action: " + dto.getAction()));
            }
        }

        String redirectUrl = (referer != null) ? referer : "/products";

        return actionMono.thenReturn("redirect:" + redirectUrl);
    }

}

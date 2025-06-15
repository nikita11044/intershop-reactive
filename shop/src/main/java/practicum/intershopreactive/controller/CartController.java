package practicum.intershopreactive.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import practicum.intershopreactive.dto.cart.CartActionFormDto;
import practicum.intershopreactive.service.CartService;
import practicum.intershopreactive.util.ActionType;
import practicum.intershopreactive.util.SecurityHelper;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public Mono<String> getCart(Authentication authentication, Model model) {
        return cartService.getAllCartItems()
                .map(cart -> {
                    model.addAttribute("items", cart.getItems());
                    model.addAttribute("total", cart.getTotal());
                    model.addAttribute("empty", cart.isEmpty());
                    model.addAttribute("canBuy", cart.isCanBuy());
                    model.addAttribute("available", cart.isAvailable());
                    model.addAttribute("role", SecurityHelper.getRole(authentication));
                    return "cart";
                });
    }

    @PostMapping("/items/{productId}")
    public Mono<String> modifyCartItem(
            @PathVariable Long productId,
            @ModelAttribute CartActionFormDto dto
    ) {
        ActionType actionType;

        actionType = ActionType.valueOf(dto.getAction().toUpperCase());

        Mono<Void> actionMono;

        switch (actionType) {
            case PLUS -> actionMono = cartService.addProduct(productId);
            case MINUS -> actionMono = cartService.removeProduct(productId);
            case DELETE -> actionMono = cartService.deleteProduct(productId);
            default -> actionMono = Mono.error(new IllegalArgumentException("Unknown action: " + dto.getAction()));
        }

        return actionMono.thenReturn("redirect:/cart");
    }

    @DeleteMapping("/items/{productId}")
    public Mono<String> deleteCartItem(@PathVariable Long productId) {
        return cartService.deleteProduct(productId)
                .thenReturn("redirect:/cart");
    }
}

package practicum.intershopreactive.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import practicum.intershopreactive.service.OrderService;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Mono<String> createOrder() {
        return orderService.purchaseCart()
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
    }

    @GetMapping
    public Mono<String> listOrders(Model model) {
        return orderService.findAllWithItemsAndProducts()
                .collectList()
                .map(orders -> {
                    model.addAttribute("orders", orders);
                    return "orders";
                });
    }

    @GetMapping("/{id}")
    public Mono<String> getOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            Model model) {

        return orderService.getOrderById(id)
                .map(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("id", id);
                    model.addAttribute("items", order.getItems());
                    model.addAttribute("newOrder", newOrder);
                    return "order";
                });
    }
}

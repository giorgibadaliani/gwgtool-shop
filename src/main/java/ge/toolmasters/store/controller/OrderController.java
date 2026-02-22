package ge.toolmasters.store.controller;

import ge.toolmasters.store.dto.CartItem;
import ge.toolmasters.store.entity.Order;
import ge.toolmasters.store.repository.OrderRepository;
import ge.toolmasters.store.service.CartService;
import ge.toolmasters.store.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class OrderController {

    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final ProductService productService;

    public OrderController(CartService cartService,
                           OrderRepository orderRepository,
                           ProductService productService) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @GetMapping("/checkout")
    public String showCheckoutForm(Model model) {
        if (cartService.getItems().isEmpty()) {
            return "redirect:/";
        }
        model.addAttribute("order", new Order());
        model.addAttribute("total", cartService.getTotalAmount());
        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@ModelAttribute Order order) {
        order.setTotalAmount(cartService.getTotalAmount());

        StringBuilder itemsText = new StringBuilder();
        for (CartItem item : cartService.getItems()) {
            // ✅ სახელი + SKU + რაოდენობა
            itemsText.append(item.getProduct().getName())
                    .append(" [SKU: ").append(item.getProduct().getSku()).append("]")
                    .append(" (").append(item.getQuantity()).append("), ");

            // ✅ სტოკის შემცირება
            productService.reduceStock(
                    item.getProduct().getId(),
                    item.getQuantity()
            );
        }
        order.setItemsDescription(itemsText.toString());

        orderRepository.save(order);
        cartService.clearCart();

        return "redirect:/order-success";
    }

    @GetMapping("/order-success")
    public String showSuccess() {
        return "order_success";
    }

    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderRepository.findAll());
        return "orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        orderRepository.findById(id).ifPresent(order -> {
            order.setStatus(status);
            orderRepository.save(order);
        });
        return "redirect:/orders";
    }

    @PostMapping("/orders/{id}/delete")
    public String deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
        return "redirect:/orders";
    }
}

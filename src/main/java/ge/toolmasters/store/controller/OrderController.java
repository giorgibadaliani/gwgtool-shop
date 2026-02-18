package ge.toolmasters.store.controller;

import ge.toolmasters.store.dto.CartItem;
import ge.toolmasters.store.entity.Order;
import ge.toolmasters.store.repository.OrderRepository;
import ge.toolmasters.store.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class OrderController {

    private final CartService cartService;
    private final OrderRepository orderRepository;

    public OrderController(CartService cartService, OrderRepository orderRepository) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
    }

    // 1. Checkout ფორმის გახსნა
    @GetMapping("/checkout")
    public String showCheckoutForm(Model model) {
        if (cartService.getItems().isEmpty()) {
            return "redirect:/"; // ცარიელი კალათით ყიდვა არ შეიძლება
        }

        model.addAttribute("order", new Order());
        model.addAttribute("total", cartService.getTotalAmount());
        return "checkout";
    }

    // 2. შეკვეთის გაგზავნა
    @PostMapping("/checkout")
    public String placeOrder(@ModelAttribute Order order) {
        // შეკვეთის დეტალების შევსება კალათიდან
        order.setTotalAmount(cartService.getTotalAmount());

        // ნივთების სიის ტექსტად ქცევა (მაგ: "Drill (1), Saw (2)")
        StringBuilder itemsText = new StringBuilder();
        for (CartItem item : cartService.getItems()) {
            itemsText.append(item.getProduct().getName())
                    .append(" (").append(item.getQuantity()).append("), ");
        }
        order.setItemsDescription(itemsText.toString());

        // ბაზაში შენახვა
        orderRepository.save(order);

        // კალათის გასუფთავება
        cartService.clearCart();

        return "redirect:/order-success"; // მადლობის გვერდი
    }

    // 3. მადლობის გვერდი
    @GetMapping("/order-success")
    public String showSuccess() {
        return "order_success";
    }

    // 4. ადმინის გვერდი: ყველა შეკვეთა
    @GetMapping("/orders")
    public String listOrders(Model model) {
        // რეპოზიტორიდან მომაქვს ყველა შეკვეთა (უახლესი ზემოთ)
        // ჯერ სორტირებას არ ვაკეთებთ, უბრალოდ ყველა წამოვიღოთ
        model.addAttribute("orders", orderRepository.findAll());
        return "orders";
    }

    // 1. სტატუსის შეცვლა (მაგ: NEW -> SHIPPED)
    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        // ვპოულობთ შეკვეთას და ვცვლით სტატუსს
        orderRepository.findById(id).ifPresent(order -> {
            order.setStatus(status);
            orderRepository.save(order);
        });
        return "redirect:/orders";
    }

    // 2. შეკვეთის წაშლა (თუ სატესტოა ან გაუქმდა)
    @PostMapping("/orders/{id}/delete")
    public String deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
        return "redirect:/orders";
    }


}

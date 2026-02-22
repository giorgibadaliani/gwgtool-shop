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
    private final ProductService productService; // ← ახალი!

    // Constructor-ი განაახლე:
    public OrderController(CartService cartService,
                           OrderRepository orderRepository,
                           ProductService productService) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.productService = productService; // ← ახალი!
    }

    // placeOrder მეთოდი — დაამატე სტოკის შემცირება:
    @PostMapping("/checkout")
    public String placeOrder(@ModelAttribute Order order) {
        order.setTotalAmount(cartService.getTotalAmount());

        StringBuilder itemsText = new StringBuilder();
        for (CartItem item : cartService.getItems()) {
            itemsText.append(item.getProduct().getName())
                    .append(" (").append(item.getQuantity()).append("), ");

            // ← ეს ახალი ხაზები: სტოკის შემცირება თითოეული ნივთისთვის
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

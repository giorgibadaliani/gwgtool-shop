package ge.toolmasters.store.controller;

import ge.toolmasters.store.dto.CartItem;
import ge.toolmasters.store.entity.Order;
import ge.toolmasters.store.repository.OrderRepository;
import ge.toolmasters.store.service.BogPaymentService;
import ge.toolmasters.store.service.CartService;
import ge.toolmasters.store.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

@Controller
public class OrderController {

    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final BogPaymentService bogPaymentService; // <-- დავამატეთ ბანკის სერვისი

    public OrderController(CartService cartService,
                           OrderRepository orderRepository,
                           ProductService productService,
                           BogPaymentService bogPaymentService) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.bogPaymentService = bogPaymentService;
    }

    // 🌟 ეს არის ბანკში გადამყვანი ახალი ლოგიკა (ფორმის ნაცვლად პირდაპირ ბანკში მიდის) 🌟
    @GetMapping("/checkout")
    public RedirectView processCheckout() {
        double totalAmount = cartService.getTotal(); // გასწორდა getTotalAmount()

        if (totalAmount <= 0) {
            return new RedirectView("/cart");
        }

        // ვქმნით უნიკალურ Order ID-ს ამ შეკვეთისთვის
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 1. ვთხოვთ ბანკს გადახდის ლინკს
        String paymentUrl = bogPaymentService.createOrder(totalAmount, orderId);

        if (paymentUrl != null) {
            // 2. თუ ბანკმა ლინკი მოგვცა, კლიენტს პირდაპირ ბანკის გვერდზე ვაგზავნით
            return new RedirectView(paymentUrl);
        } else {
            return new RedirectView("/cart?error=payment_failed");
        }
    }

    // 🌟 როცა კლიენტი გადაიხდის და ბანკი უკან დააბრუნებს 🌟
    @GetMapping("/payment/success")
    public String paymentSuccess() {
        // ბანკიდან დაბრუნებისთანავე ვინახავთ შეკვეთას ბაზაში
        Order order = new Order();
        order.setTotalAmount(cartService.getTotal()); // გასწორდა getTotalAmount()
        order.setStatus("გადახდილია 💳"); // ახალი სტატუსი

        // (რადგან ბანკის გვერდიდან სახელი და მისამართი არ მოდის, აქ დროებით "Bank Payment" ჩავწეროთ)
        order.setCustomerName("ონლაინ გადახდა");

        StringBuilder itemsText = new StringBuilder();
        for (CartItem item : cartService.getItems()) {
            itemsText.append(item.getProduct().getName())
                    .append(" [SKU: ").append(item.getProduct().getSku()).append("]")
                    .append(" (").append(item.getQuantity()).append("), ");

            // სტოკის შემცირება
            productService.reduceStock(
                    item.getProduct().getId(),
                    item.getQuantity()
            );
        }
        order.setItemsDescription(itemsText.toString());

        orderRepository.save(order);
        cartService.clearCart(); // ვასუფთავებთ კალათას

        return "redirect:/order-success";
    }

    // 🌟 როცა გააუქმებს ბანკის გვერდზე 🌟
    @GetMapping("/payment/fail")
    public String paymentFail() {
        return "redirect:/cart?payment=fail";
    }

    // ----------------------------------------------------
    // ძველი მეთოდები, რაც უკვე გქონდა
    // ----------------------------------------------------

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

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
    private final BogPaymentService bogPaymentService;

    // დროებით ვინახავთ მიმდინარე შეკვეთას სესიაში (ან კონტროლერში), სანამ ბანკიდან დაბრუნდება
    private Order pendingOrder;

    public OrderController(CartService cartService,
                           OrderRepository orderRepository,
                           ProductService productService,
                           BogPaymentService bogPaymentService) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.bogPaymentService = bogPaymentService;
    }

    // ხსნის ფორმას (სახელი, გვარი, მისამართი)
    @GetMapping("/checkout")
    public String showCheckoutForm(Model model) {
        if (cartService.getItems().isEmpty()) {
            return "redirect:/cart";
        }
        model.addAttribute("order", new Order());
        model.addAttribute("total", cartService.getTotal());
        return "checkout";
    }

    // როცა ფორმას ავსებს და აგზავნის (Post)
    @PostMapping("/checkout")
    public Object placeOrder(@ModelAttribute Order order, @RequestParam(required = false) String paymentMethod) {

        double totalAmount = cartService.getTotal();
        if (totalAmount <= 0) return "redirect:/cart";

        order.setTotalAmount(totalAmount);

        // ვაწყობთ კალათის ნივთების აღწერას
        StringBuilder itemsText = new StringBuilder();
        for (CartItem item : cartService.getItems()) {
            itemsText.append(item.getProduct().getName())
                    .append(" [SKU: ").append(item.getProduct().getSku()).append("]")
                    .append(" (").append(item.getQuantity()).append("), ");
        }
        order.setItemsDescription(itemsText.toString());

        // 🌟 თუ აირჩია ონლაინ გადახდა (BOG) 🌟
        if ("ONLINE".equals(paymentMethod)) {
            order.setStatus("ონლაინ გადახდის მოლოდინში ⏳");
            this.pendingOrder = order; // ვინახავთ დროებით მეხსიერებაში

            String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String paymentUrl = bogPaymentService.createOrder(totalAmount, orderId);

            if (paymentUrl != null) {
                return new RedirectView(paymentUrl); // უშვებს ბანკში
            } else {
                return "redirect:/cart?error=payment_failed";
            }
        }

        // 🌟 თუ აირჩია ჩვეულებრივი (ნაღდი/გადარიცხვა) 🌟
        else {
            order.setStatus("ახალი შეკვეთა 🆕");

            // სტოკის შემცირება მხოლოდ მაშინ, როცა უკვე საბოლოოდ გაფორმდება
            for (CartItem item : cartService.getItems()) {
                productService.reduceStock(item.getProduct().getId(), item.getQuantity());
            }

            orderRepository.save(order);
            cartService.clearCart();
            return "redirect:/order-success";
        }
    }

    // 🌟 როცა ბანკიდან წარმატებით დაბრუნდება 🌟
    @GetMapping("/payment/success")
    public String paymentSuccess() {
        if (this.pendingOrder != null) {
            this.pendingOrder.setStatus("გადახდილია 💳"); // ვუცვლით სტატუსს

            // ვაკლებთ მარაგს
            for (CartItem item : cartService.getItems()) {
                productService.reduceStock(item.getProduct().getId(), item.getQuantity());
            }

            orderRepository.save(this.pendingOrder); // ვინახავთ ბაზაში ონლაინ გადახდილ შეკვეთას
            this.pendingOrder = null; // ვასუფთავებთ მეხსიერებას
        }

        cartService.clearCart();
        return "redirect:/order-success";
    }

    // 🌟 როცა ბანკიდან გაუქმებული დაბრუნდება 🌟
    @GetMapping("/payment/fail")
    public String paymentFail() {
        this.pendingOrder = null; // ვშლით დროებით შეკვეთას
        return "redirect:/cart?error=payment_cancelled";
    }

    // ძველი მეთოდები...
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

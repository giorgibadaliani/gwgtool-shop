package ge.toolmasters.store.controller;

import ge.toolmasters.store.service.BogPaymentService;
import ge.toolmasters.store.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

@Controller
public class CheckoutController {

    private final BogPaymentService bogPaymentService;
    private final CartService cartService;

    public CheckoutController(BogPaymentService bogPaymentService, CartService cartService) {
        this.bogPaymentService = bogPaymentService;
        this.cartService = cartService;
    }

    // როცა კლიენტი აჭერს "ყიდვა"-ს კალათაში
    @GetMapping("/checkout")
    public RedirectView processCheckout() {
        double totalAmount = cartService.getTotal();

        if (totalAmount <= 0) {
            return new RedirectView("/cart"); // თუ კალათა ცარიელია, ვაბრუნებთ უკან
        }

        // ვქმნით უნიკალურ Order ID-ს ამ შეკვეთისთვის (გასწორდა UUID-ის ერორი)
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // ვთხოვთ ბანკს გადახდის ლინკს
        String paymentUrl = bogPaymentService.createOrder(totalAmount, orderId);

        if (paymentUrl != null) {
            // თუ ბანკმა ლინკი მოგვცა, კლიენტს პირდაპირ ბანკის გვერდზე ვაგზავნით
            return new RedirectView(paymentUrl);
        } else {
            // თუ რამე ერორი მოხდა, კალათაში ვაბრუნებთ
            return new RedirectView("/cart?error=payment_failed");
        }
    }

    // როცა გადაიხდის და ბანკი უკან დააბრუნებს
    @GetMapping("/payment/success")
    public String paymentSuccess() {
        cartService.clearCart(); // გადაიხადა -> ვუსუფთავებთ კალათას
        return "redirect:/?payment=success"; // მთავარ გვერდზე ვუშვებთ
    }

    // როცა გააუქმებს ბანკის გვერდზე
    @GetMapping("/payment/fail")
    public String paymentFail() {
        return "redirect:/cart?payment=fail"; // კალათაში ვაბრუნებთ
    }
}

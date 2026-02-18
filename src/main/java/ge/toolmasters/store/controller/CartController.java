package ge.toolmasters.store.controller;

import ge.toolmasters.store.service.CartService;
import ge.toolmasters.store.service.ProductService;
import ge.toolmasters.store.entity.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cart") // ყველა მისამართი დაიწყება /cart-ით
public class CartController {

    private final CartService cartService;
    private final ProductService productService;

    public CartController(CartService cartService, ProductService productService) {
        this.cartService = cartService;
        this.productService = productService;
    }

    // 1. კალათის ნახვა
    @GetMapping
    public String showCart(Model model) {
        // ეს ხაზი ჩაამატე index, product_details და cart მეთოდებში return-მდე
        model.addAttribute("cartCount", cartService.getItems().size());

        model.addAttribute("items", cartService.getItems());
        model.addAttribute("total", cartService.getTotalAmount());
        return "cart"; // გახსნის cart.html-ს
    }

    // 2. პროდუქტის დამატება კალათაში
    @GetMapping("/add/{id}") // მაგ: /cart/add/5
    public String addToCart(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product != null) {
            cartService.addToCart(product);
        }
        return "redirect:/cart"; // დამატების მერე გადადის კალათის გვერდზე
    }

    // 3. კალათიდან წაშლა
    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Long id) {
        cartService.removeFromCart(id);
        return "redirect:/cart";
    }

    // 4. კალათის გასუფთავება
    @GetMapping("/clear")
    public String clearCart() {

        cartService.clearCart();
        return "redirect:/cart";

    }
}

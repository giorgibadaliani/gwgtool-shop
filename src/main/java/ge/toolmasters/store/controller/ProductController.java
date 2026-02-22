package ge.toolmasters.store.controller;

import ge.toolmasters.store.entity.Product;
import ge.toolmasters.store.service.CartService;
import ge.toolmasters.store.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
public class ProductController {

    private final ProductService productService;
    private final CartService cartService;

    public ProductController(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
    }

    // --- მყიდველის ნაწილი ---

    // 1. მთავარი გვერდი (ვიტრინა)
    @GetMapping("/")
    public String showShop(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("cartCount", cartService.getItems().size());
        return "index";
    }

    // 2. პროდუქტის დეტალური გვერდი
    @GetMapping("/product/{id}")
    public String showProductDetails(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return "redirect:/";
        }
        model.addAttribute("product", product);
        model.addAttribute("cartCount", cartService.getItems().size());
        return "product_details";
    }

    // --- ადმინის ნაწილი ---

    // 3. ადმინ პანელი - პროდუქტების სია
    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "products";
    }

    // 4. ახალი პროდუქტის დამატების გვერდი (ფორმა)
    @GetMapping("/products/new")
    public String createProductForm(Model model) {
        model.addAttribute("product", new Product());
        return "create_product";
    }

    // 5. პროდუქტის შენახვა (ახალის ან რედაქტირებულის)
    // ✅ required=false — თუ ფაილი არ ატვირთეს, 500 error არ ხდება
    @PostMapping("/products")
    public String saveProduct(
            @ModelAttribute("product") Product product,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile)
            throws IOException {

        // თუ ფაილი ატვირთეს — ფაილი ინახება
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = productService.uploadImage(imageFile);
            product.setImageUrl(fileName);
        }
        // თუ ფაილი არ ატვირთეს — imageUrl (URL ველი) რჩება

        productService.saveProduct(product);
        return "redirect:/products";
    }

    // 6. რედაქტირების გვერდის გახსნა
    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        return "create_product";
    }

    // 7. წაშლის ბრძანება
    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "redirect:/products";
    }
}

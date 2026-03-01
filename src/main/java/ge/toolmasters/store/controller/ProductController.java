package ge.toolmasters.store.controller;

import ge.toolmasters.store.entity.Product;
import ge.toolmasters.store.entity.ProductCharacteristic;
import ge.toolmasters.store.repository.ProductCharacteristicRepository;
import ge.toolmasters.store.service.CartService;
import ge.toolmasters.store.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Controller
public class ProductController {

    private final ProductService productService;
    private final CartService cartService;
    private final ProductCharacteristicRepository characteristicRepo;

    public ProductController(ProductService productService, CartService cartService, ProductCharacteristicRepository characteristicRepo) {
        this.productService = productService;
        this.cartService = cartService;
        this.characteristicRepo = characteristicRepo;
    }

    @GetMapping("/")
    public String showShop(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("cartCount", cartService.getItems().size());
        return "index";
    }

    @GetMapping("/product/{id}")
    public String showProductDetails(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return "redirect:/";
        }

        // ვიღებთ მახასიათებლებს
        List<ProductCharacteristic> characteristics = characteristicRepo.findBySku(product.getSku());

        model.addAttribute("product", product);
        model.addAttribute("characteristics", characteristics);
        model.addAttribute("cartCount", cartService.getItems().size());
        return "product_details";
    }

    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "products";
    }

    // --- მეთოდი კატეგორიებისთვის ---
    @GetMapping("/category/{categoryName}")
    public String showCategory(@PathVariable("categoryName") String categoryName, Model model) {
        try {
            Product.Category category = Product.Category.valueOf(categoryName.toUpperCase());
            List<Product> products = productService.getProductsByCategory(category);

            model.addAttribute("products", products);
            model.addAttribute("cartCount", cartService.getItems().size());
            return "index";
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }
    }

    @GetMapping("/products/new")
    public String createProductForm(Model model) {
        model.addAttribute("product", new Product());
        return "create_product";
    }

    @PostMapping("/products/save")
    public String saveProduct(
            @ModelAttribute("product") Product product,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        try {
            // 1. თუ პროდუქტი უკვე არსებობს (რედაქტირებაა)
            if (product.getId() != null) {
                Product existingProduct = productService.getProductById(product.getId());
                if (existingProduct != null) {

                    // თუ SKU და Description ცარიელია, ძველი შევინარჩუნოთ
                    if (product.getSku() == null) product.setSku(existingProduct.getSku());
                    if (product.getDescription() == null || product.getDescription().isEmpty()) {
                        product.setDescription(existingProduct.getDescription());
                    }

                    // 🚨 სურათის ლოგიკა:
                    // თუ მომხმარებელმა ახალი ფაილი არ ატვირთა და არც ახალი ლინკი ჩაწერა
                    // (ანუ ფორმიდან მოსული imageUrl ცარიელია), მაშინ დავუტოვოთ ძველი სურათი.
                    if ((imageFile == null || imageFile.isEmpty()) &&
                            (product.getImageUrl() == null || product.getImageUrl().isEmpty())) {
                        product.setImageUrl(existingProduct.getImageUrl());
                    }
                }
            }

            // 2. თუ ახალი ფაილი ატვირთა, ის ყოველთვის "მოიგებს" და გადააწერს ლინკს
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = productService.uploadImage(imageFile);
                product.setImageUrl(fileName);
            }
            // 3. თუ ფაილი არ აუტვირთავს, მაგრამ ლინკი ჩააგდო ტექსტურ ველში,
            // product.getImageUrl() უკვე შეიცავს მაგ ლინკს (რადგან @ModelAttribute-მა თავისით ჩასვა)
            // და პირდაპირ ეგ შეინახება!

            productService.saveProduct(product);
            return "redirect:/products";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/products?error";
        }
    }


    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        return "create_product";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "redirect:/products";
    }
}

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

    // 🚨 შეცვლილი მთავარი მეთოდი: ახლა იღებს ფილტრებს!
    @GetMapping("/")
    public String showShop(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String voltage,
            @RequestParam(required = false) Boolean isBrushless,
            @RequestParam(required = false) Boolean isToolOnly,
            Model model) {

        // კატეგორიის სტრინგის გადაყვანა Enum-ში თუ მოწოდებულია
        Product.Category catEnum = null;
        if (category != null && !category.isEmpty()) {
            try {
                catEnum = Product.Category.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                // თუ არასწორი კატეგორია ჩაწერეს URL-ში, უბრალოდ იგნორირებას ვუკეთებთ
            }
        }

        // ვიძახებთ ჩვენს ახალ ჭკვიან ფილტრს
        List<Product> products = productService.filterProducts(
                catEnum, minPrice, maxPrice, voltage, isBrushless, isToolOnly
        );

        model.addAttribute("products", products);
        model.addAttribute("cartCount", cartService.getItems().size());

        // ვაგზავნით არჩეულ ფილტრებს უკან HTML-ში, რომ მონიშნული დარჩეს სლაიდერები და ჩექბოქსები
        model.addAttribute("selectedCategory", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedVoltage", voltage);
        model.addAttribute("selectedBrushless", isBrushless);
        model.addAttribute("selectedToolOnly", isToolOnly);

        return "index";
    }

    @GetMapping("/product/{id}")
    public String showProductDetails(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return "redirect:/";
        }

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

    // ეს მეთოდი დავტოვე ძველი ბმულებისთვის, მაგრამ ისიც ჩვენს ახალ ფილტრს გამოიყენებს
    @GetMapping("/category/{categoryName}")
    public String showCategory(@PathVariable("categoryName") String categoryName, Model model) {
        return showShop(categoryName, null, null, null, null, null, model);
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
            if (product.getId() != null) {
                Product existingProduct = productService.getProductById(product.getId());
                if (existingProduct != null) {
                    if (product.getSku() == null) product.setSku(existingProduct.getSku());
                    if (product.getDescription() == null || product.getDescription().isEmpty()) {
                        product.setDescription(existingProduct.getDescription());
                    }
                    if ((imageFile == null || imageFile.isEmpty()) &&
                            (product.getImageUrl() == null || product.getImageUrl().isEmpty())) {
                        product.setImageUrl(existingProduct.getImageUrl());
                    }
                }
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = productService.uploadImage(imageFile);
                product.setImageUrl(fileName);
            }

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

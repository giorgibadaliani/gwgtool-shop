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
    public String showShop(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String voltage,
            @RequestParam(required = false) Boolean isBrushless,
            @RequestParam(required = false) Boolean isToolOnly,
            @RequestParam(required = false) String sku, // ✅ ახალი დამატებული SKU ძებნა
            Model model) {

        List<Product> products;

        // ✅ თუ SKU ჩაწერილია, მხოლოდ მაგით ვეძებთ და სხვა ფილტრებს ვაიგნორებთ
        if (sku != null && !sku.trim().isEmpty()) {
            products = productService.searchProductsBySku(sku);
        } else {
            // წინააღმდეგ შემთხვევაში ვამუშავებთ სტანდარტულ ფილტრაციას
            Product.Category catEnum = null;
            if (category != null && !category.isEmpty()) {
                try {
                    catEnum = Product.Category.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                }
            }
            products = productService.filterProducts(
                    catEnum, minPrice, maxPrice, voltage, isBrushless, isToolOnly
            );
        }

        model.addAttribute("products", products);
        model.addAttribute("cartCount", cartService.getItems().size());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedVoltage", voltage);
        model.addAttribute("selectedBrushless", isBrushless);
        model.addAttribute("selectedToolOnly", isToolOnly);
        model.addAttribute("searchedSku", sku); // ✅ HTML-ში რომ შევინარჩუნოთ ჩაწერილი ტექსტი

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
    public String listProducts(@RequestParam(required = false) String sku, Model model) { // ✅ SKU პარამეტრი დაემატა ადმინშიც
        List<Product> products;

        if (sku != null && !sku.trim().isEmpty()) {
            products = productService.searchProductsBySku(sku);
        } else {
            products = productService.getAllProducts();
        }

        model.addAttribute("products", products);
        model.addAttribute("searchedSku", sku); // ✅ HTML-ისთვის
        return "products";
    }

    @GetMapping("/category/{categoryName}")
    public String showCategory(@PathVariable("categoryName") String categoryName, Model model) {
        return showShop(categoryName, null, null, null, null, null, null, model); // ✅ დაემატა 1 null (sku-სთვის)
    }

    @GetMapping("/products/new")
    public String createProductForm(Model model) {
        model.addAttribute("product", new Product());
        return "create_product";
    }

    // 🌟 შეცვლილი, დაცული შენახვის მეთოდი
    @PostMapping("/products/save")
    public String saveProduct(
            @ModelAttribute("product") Product product,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        // თუ ფასდაკლების ველი მოვიდა 0 ან უარყოფითი, ვაქცევთ null-ად (რომ შეცდომა არ ამოაგდოს)
        if (product.getDiscountPercentage() != null && product.getDiscountPercentage() <= 0) {
            product.setDiscountPercentage(null);
        }

        // თუ Voltage ცარიელი ტექსტია, null-ად ვაქცევთ
        if (product.getVoltage() != null && product.getVoltage().trim().isEmpty()) {
            product.setVoltage(null);
        }

        // არსებული პროდუქტის განახლების ლოგიკა
        if (product.getId() != null) {
            Product existingProduct = productService.getProductById(product.getId());
            if (existingProduct != null) {
                // თუ SKU ფორმაში არ იყო, ძველს ვტოვებთ
                if (product.getSku() == null || product.getSku().trim().isEmpty()) {
                    product.setSku(existingProduct.getSku());
                }
                // თუ აღწერა არ იყო, ძველს ვტოვებთ
                if (product.getDescription() == null || product.getDescription().trim().isEmpty()) {
                    product.setDescription(existingProduct.getDescription());
                }
                // მარაგის დაცვა (რომ შენახვისას არ გაქრეს)
                if (product.getStockQuantity() == null) {
                    product.setStockQuantity(existingProduct.getStockQuantity());
                }
                // სურათის დაცვა
                if ((imageFile == null || imageFile.isEmpty()) &&
                        (product.getImageUrl() == null || product.getImageUrl().trim().isEmpty())) {
                    product.setImageUrl(existingProduct.getImageUrl());
                }
            }
        }

        // ახალი სურათის ატვირთვა
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = productService.uploadImage(imageFile);
                product.setImageUrl(fileName);
            }
        } catch (Exception e) {
            // თუ სურათი ვერ აიტვირთა, უბრალოდ ლოგში დავწერთ და ბაზის შენახვას გავაგრძელებთ
            System.err.println("სურათის ატვირთვა ვერ მოხერხდა: " + e.getMessage());
        }

        // ბაზაში შენახვა (try-catch აღარ გვინდა, რადგან ყველა null მნიშვნელობა დაზღვეულია)
        productService.saveProduct(product);

        return "redirect:/products";
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

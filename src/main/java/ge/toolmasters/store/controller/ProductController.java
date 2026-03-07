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
import java.util.stream.Collectors;

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
            @RequestParam(required = false) String sku,
            Model model) {

        // 💡 ბაგის შესწორება: თუ Voltage "ყველა" აირჩიეს, ვაქცევთ null-ად
        if (voltage != null && (voltage.trim().isEmpty() || voltage.equalsIgnoreCase("ყველა") || voltage.equalsIgnoreCase("ALL"))) {
            voltage = null;
        }

        List<Product> products;

        if (sku != null && !sku.trim().isEmpty()) {
            products = productService.searchProductsBySku(sku);
        } else {
            Product.Category catEnum = null;
            if (category != null && !category.isEmpty()) {
                try {
                    catEnum = Product.Category.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                }
            }
            // ბაზიდან მოაქვს გაფილტრული პროდუქტი
            products = productService.filterProducts(
                    catEnum, minPrice, maxPrice, voltage, isBrushless, isToolOnly
            );
        }

        // ფასების ჭკვიანი ფილტრაცია
        if (minPrice != null || maxPrice != null) {
            products = products.stream().filter(product -> {
                double actualPrice = (product.getDiscountPercentage() != null && product.getDiscountPercentage() > 0)
                        ? product.getDiscountedPrice()
                        : product.getPrice();

                boolean passesMin = (minPrice == null || actualPrice >= minPrice);
                boolean passesMax = (maxPrice == null || actualPrice <= maxPrice);

                return passesMin && passesMax;
            }).collect(Collectors.toList());
        }

        model.addAttribute("products", products);
        model.addAttribute("cartCount", cartService.getItems().size());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedVoltage", voltage);
        model.addAttribute("selectedBrushless", isBrushless);
        model.addAttribute("selectedToolOnly", isToolOnly);
        model.addAttribute("searchedSku", sku);

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
    public String listProducts(@RequestParam(required = false) String sku, Model model) {
        List<Product> products;

        if (sku != null && !sku.trim().isEmpty()) {
            products = productService.searchProductsBySku(sku);
        } else {
            products = productService.getAllProducts();
        }

        model.addAttribute("products", products);
        model.addAttribute("searchedSku", sku);
        return "products";
    }

    @GetMapping("/category/{categoryName}")
    public String showCategory(@PathVariable("categoryName") String categoryName, Model model) {
        return showShop(categoryName, null, null, null, null, null, null, model);
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

        // 💡 ჩეკბოქსების დაზღვევა (რომ null-ები არ შევიდეს ბაზაში)
        if (product.getHasBattery() == null) product.setHasBattery(false);
        if (product.getHasCharger() == null) product.setHasCharger(false);
        if (product.getHasCase() == null) product.setHasCase(false);

        if (product.getDiscountPercentage() != null && product.getDiscountPercentage() <= 0) {
            product.setDiscountPercentage(null);
        }

        if (product.getVoltage() != null && product.getVoltage().trim().isEmpty()) {
            product.setVoltage(null);
        }

        // 💡 ძველი ველების შენარჩუნება რედაქტირებისას
        if (product.getId() != null) {
            Product existingProduct = productService.getProductById(product.getId());
            if (existingProduct != null) {
                if (product.getSku() == null || product.getSku().trim().isEmpty()) {
                    product.setSku(existingProduct.getSku());
                }
                if (product.getDescription() == null || product.getDescription().trim().isEmpty()) {
                    product.setDescription(existingProduct.getDescription());
                }
                if (product.getStockQuantity() == null) {
                    product.setStockQuantity(existingProduct.getStockQuantity());
                }
                if ((imageFile == null || imageFile.isEmpty()) &&
                        (product.getImageUrl() == null || product.getImageUrl().trim().isEmpty())) {
                    product.setImageUrl(existingProduct.getImageUrl());
                }
                // ვიცავთ ფარულ ველებს გაქრობისგან
                if (product.getIsToolOnly() == null) {
                    product.setIsToolOnly(existingProduct.getIsToolOnly());
                }
                if (product.getIsBrushless() == null) {
                    product.setIsBrushless(existingProduct.getIsBrushless());
                }
            }
        }

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = productService.uploadImage(imageFile);
                product.setImageUrl(fileName);
            }
        } catch (Exception e) {
            System.err.println("სურათის ატვირთვა ვერ მოხერხდა: " + e.getMessage());
        }

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

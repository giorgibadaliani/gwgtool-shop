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

import java.util.ArrayList;
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
            @RequestParam(required = false, value = "isBrushless") String isBrushlessStr,
            @RequestParam(required = false, value = "isToolOnly") String isToolOnlyStr,
            @RequestParam(required = false) String sku,
            Model model) {

        // 🔥 1. უხეში ძალა: ვიღებთ ყველა პროდუქტს, რაც კი არსებობს!
        List<Product> products = productService.getAllProducts();

        // თუ რატომღაც null მოვიდა, ცარიელ ლისტად ვაქცევთ რომ არ გაჭედოს
        if (products == null) {
            products = new ArrayList<>();
        }

        // 2. ძებნა SKU-თი (თუ მითითებულია)
        if (sku != null && !sku.trim().isEmpty()) {
            final String searchSku = sku.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> p.getSku() != null && p.getSku().toLowerCase().contains(searchSku))
                    .collect(Collectors.toList());
        } else {
            // 3. თუ SKU არ არის, ვაკეთებთ ფილტრაციას

            // კატეგორიის ფილტრი
            if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ყველა") && !category.equalsIgnoreCase("ALL")) {
                try {
                    Product.Category catEnum = Product.Category.valueOf(category.toUpperCase());
                    products = products.stream()
                            .filter(p -> p.getCategory() != null && p.getCategory() == catEnum)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) { }
            }

            // ვოლტაჟის ფილტრი
            if (voltage != null && !voltage.trim().isEmpty() && !voltage.equalsIgnoreCase("ყველა") && !voltage.equalsIgnoreCase("ALL")) {
                final String finalVoltage = voltage.trim();
                products = products.stream()
                        .filter(p -> p.getVoltage() != null && p.getVoltage().equalsIgnoreCase(finalVoltage))
                        .collect(Collectors.toList());
            }

            // უნახშირო (Brushless) ფილტრი
            if (isBrushlessStr != null && (isBrushlessStr.equalsIgnoreCase("true") || isBrushlessStr.equalsIgnoreCase("false"))) {
                boolean searchBrushless = Boolean.parseBoolean(isBrushlessStr);
                products = products.stream().filter(p -> {
                    boolean actual = (p.getIsBrushless() != null) ? p.getIsBrushless() : false;
                    return actual == searchBrushless;
                }).collect(Collectors.toList());
            }

            // კარკასის (Tool Only) ფილტრი
            if (isToolOnlyStr != null && (isToolOnlyStr.equalsIgnoreCase("true") || isToolOnlyStr.equalsIgnoreCase("false"))) {
                boolean searchToolOnly = Boolean.parseBoolean(isToolOnlyStr);
                products = products.stream().filter(p -> {
                    boolean actual = (p.getIsToolOnly() != null) ? p.getIsToolOnly() : false;
                    return actual == searchToolOnly;
                }).collect(Collectors.toList());
            }

            // ფასების ფილტრი
            if (minPrice != null || maxPrice != null) {
                products = products.stream().filter(p -> {
                    if (p.getPrice() == null) return false; // თუ ფასი საერთოდ არ უწერია, ვმალავთ რომ არ გაჭედოს

                    double actualPrice = (p.getDiscountPercentage() != null && p.getDiscountPercentage() > 0)
                            ? p.getDiscountedPrice()
                            : p.getPrice();

                    boolean passesMin = (minPrice == null || actualPrice >= minPrice);
                    boolean passesMax = (maxPrice == null || actualPrice <= maxPrice);

                    return passesMin && passesMax;
                }).collect(Collectors.toList());
            }
        }

        // HTML-ისთვის Boolean მნიშვნელობების მომზადება
        Boolean parsedBrushless = null;
        if (isBrushlessStr != null && (isBrushlessStr.equalsIgnoreCase("true") || isBrushlessStr.equalsIgnoreCase("false"))) {
            parsedBrushless = Boolean.parseBoolean(isBrushlessStr);
        }

        Boolean parsedToolOnly = null;
        if (isToolOnlyStr != null && (isToolOnlyStr.equalsIgnoreCase("true") || isToolOnlyStr.equalsIgnoreCase("false"))) {
            parsedToolOnly = Boolean.parseBoolean(isToolOnlyStr);
        }

        model.addAttribute("products", products);
        model.addAttribute("cartCount", cartService.getItems() != null ? cartService.getItems().size() : 0);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedVoltage", voltage);
        model.addAttribute("selectedBrushless", parsedBrushless);
        model.addAttribute("selectedToolOnly", parsedToolOnly);
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

        if (product.getHasBattery() == null) product.setHasBattery(false);
        if (product.getHasCharger() == null) product.setHasCharger(false);
        if (product.getHasCase() == null) product.setHasCase(false);

        if (product.getDiscountPercentage() != null && product.getDiscountPercentage() <= 0) {
            product.setDiscountPercentage(null);
        }
        if (product.getVoltage() != null && product.getVoltage().trim().isEmpty()) {
            product.setVoltage(null);
        }

        if (product.getId() != null) {
            Product existingProduct = productService.getProductById(product.getId());
            if (existingProduct != null) {
                if (product.getSku() == null || product.getSku().trim().isEmpty()) product.setSku(existingProduct.getSku());
                if (product.getDescription() == null || product.getDescription().trim().isEmpty()) product.setDescription(existingProduct.getDescription());
                if (product.getStockQuantity() == null) product.setStockQuantity(existingProduct.getStockQuantity());
                if ((imageFile == null || imageFile.isEmpty()) && (product.getImageUrl() == null || product.getImageUrl().trim().isEmpty())) {
                    product.setImageUrl(existingProduct.getImageUrl());
                }
                if (product.getIsToolOnly() == null) product.setIsToolOnly(existingProduct.getIsToolOnly());
                if (product.getIsBrushless() == null) product.setIsBrushless(existingProduct.getIsBrushless());
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

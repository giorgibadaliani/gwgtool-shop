package ge.toolmasters.store.service;

import ge.toolmasters.store.entity.Product;
import ge.toolmasters.store.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    // საქაღალდე, სადაც სურათებს შევინახავთ (პროექტის ძირში)
    private final String uploadDir = "uploads";

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 1. ყველა პროდუქტი
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 2. ერთი პროდუქტი
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    // 3. შენახვა
    public void saveProduct(Product product) {
        productRepository.save(product);
    }

    // 4. წაშლა
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    // ProductService.java-ში დაამატე ეს მეთოდი:

    // 6. მარაგის შემცირება (შეძენის შემდეგ)
    public void reduceStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null && product.getStockQuantity() != null) {
            int newStock = product.getStockQuantity() - quantity;
            product.setStockQuantity(Math.max(0, newStock)); // 0-ზე ნაკლები არ გახდეს
            productRepository.save(product);
        }
    }



    // 5. სურათის ატვირთვა (გაერთიანებული ვერსია)
    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) return null;

        // ვქმნით საქაღალდეს თუ არ არსებობს
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // ვქმნით უნიკალურ სახელს (დრო + ორიგინალი სახელი)
        // ეს უფრო მარტივია და მოკლე ვიდრე UUID
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // ვასუფთავებთ სახელს (რომ უცნაური სიმბოლოები არ იყოს)
        fileName = fileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");

        // ვინახავთ ფაილს
        try (InputStream inputStream = file.getInputStream()) {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        return fileName;
    }
}

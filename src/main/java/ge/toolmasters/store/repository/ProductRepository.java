package ge.toolmasters.store.repository;

import ge.toolmasters.store.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    // SKU კოდის ნაწილობრივი დამთხვევით (დიდი/პატარა ასოების იგნორირებით)
    List<Product> findBySkuContainingIgnoreCase(String sku);

    // იპოვის ყველა პროდუქტს ვოლტაჟის მიხედვით (მაგ: "M18")
    List<Product> findByVoltage(String voltage);

    // იპოვის პროდუქტებს კატეგორიის მიხედვით
    List<Product> findByCategory(Product.Category category);

    // 🚨 შესწორებული ფილტრი
    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:voltage IS NULL OR p.voltage = :voltage) AND " +
            "(:isBrushless IS NULL OR p.isBrushless = :isBrushless) AND " +
            "(:isToolOnly IS NULL OR p.isToolOnly = :isToolOnly)")
    List<Product> findFilteredProducts(
            @Param("category") Product.Category category,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("voltage") String voltage,
            @Param("isBrushless") Boolean isBrushless,
            @Param("isToolOnly") Boolean isToolOnly
    );
}

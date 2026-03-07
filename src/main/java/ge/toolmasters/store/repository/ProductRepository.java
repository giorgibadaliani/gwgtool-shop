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

    List<Product> findBySkuContainingIgnoreCase(String sku);

    List<Product> findByVoltage(String voltage);

    List<Product> findByCategory(Product.Category category);

    // 🚨 იდეალურად დაცული SQL შეკითხვა 🚨
    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:voltage IS NULL OR UPPER(p.voltage) = UPPER(:voltage)) AND " +
            "(:isBrushless IS NULL OR COALESCE(p.isBrushless, false) = :isBrushless) AND " +
            "(:isToolOnly IS NULL OR COALESCE(p.isToolOnly, false) = :isToolOnly)")
    List<Product> findFilteredProducts(
            @Param("category") Product.Category category,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("voltage") String voltage,
            @Param("isBrushless") Boolean isBrushless,
            @Param("isToolOnly") Boolean isToolOnly
    );
}

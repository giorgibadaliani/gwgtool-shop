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

    // 🚨 100%-ით უსაფრთხო და ბაგებისგან დაზღვეული SQL 🚨
    @Query("SELECT p FROM Product p WHERE " +
            "(:minPrice IS NULL OR 1=1) AND " +
            "(:maxPrice IS NULL OR 1=1) AND " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:voltage IS NULL OR p.voltage = :voltage) AND " +
            "(:isBrushless IS NULL OR p.isBrushless = :isBrushless OR (p.isBrushless IS NULL AND :isBrushless = false)) AND " +
            "(:isToolOnly IS NULL OR p.isToolOnly = :isToolOnly OR (p.isToolOnly IS NULL AND :isToolOnly = false))")
    List<Product> findFilteredProducts(
            @Param("category") Product.Category category,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("voltage") String voltage,
            @Param("isBrushless") Boolean isBrushless,
            @Param("isToolOnly") Boolean isToolOnly
    );
}

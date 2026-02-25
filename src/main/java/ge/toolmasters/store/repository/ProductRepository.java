package ge.toolmasters.store.repository;

import ge.toolmasters.store.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // აქ ხდება: Spring-ი მეთოდის სახელიდან ხვდება რა SQL დაწეროს
    // ეს აუცილებელია!
    Optional<Product> findBySku(String sku);

    // იპოვის ყველა პროდუქტს ვოლტაჟის მიხედვით (მაგ: "M18")
    List<Product> findByVoltage(String voltage);

    // --- ახალი დამატებული: იპოვის პროდუქტებს კატეგორიის მიხედვით ---
    List<Product> findByCategory(Product.Category category);
}

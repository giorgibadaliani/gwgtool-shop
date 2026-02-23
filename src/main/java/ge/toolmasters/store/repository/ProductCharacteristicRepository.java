package ge.toolmasters.store.repository;

import ge.toolmasters.store.entity.ProductCharacteristic; // 🔴 ეს ხაზი არის ყველაზე მნიშვნელოვანი!
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCharacteristicRepository extends JpaRepository<ProductCharacteristic, Long> {
    void deleteBySku(String sku);
}

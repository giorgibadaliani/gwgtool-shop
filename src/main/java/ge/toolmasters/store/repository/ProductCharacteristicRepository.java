package ge.toolmasters.store.repository;

import ge.toolmasters.store.entity.ProductCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductCharacteristicRepository extends JpaRepository<ProductCharacteristic, Long> {
    List<ProductCharacteristic> findBySku(String sku);
    void deleteBySku(String sku);
}

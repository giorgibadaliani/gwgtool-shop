package ge.toolmasters.store.entity; // დარწმუნდი, რომ პაკეტი სწორია

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "product_characteristics")
public class ProductCharacteristic {

    // Getters and Setters აუცილებელია!
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private String name;
    private String value;

    public void setId(Long id) { this.id = id; }

    public void setSku(String sku) { this.sku = sku; }

    public void setName(String name) { this.name = name; }

    public void setValue(String value) { this.value = value; }
}

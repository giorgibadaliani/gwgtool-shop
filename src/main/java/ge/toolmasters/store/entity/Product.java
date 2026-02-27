package ge.toolmasters.store.entity;

import jakarta.persistence.*;
import lombok.Data; // Lombok გვეხმარება, მაგრამ აუცილებელ მეთოდებს მაინც ხელით ვწერთ დაზღვევისთვის

@Entity
@Data
@Table(name = "products")
public class Product {

    // 1. ვქმნით კატეგორიების ჩამონათვალს (Enum) პირდაპირ კლასში
    public enum Category {
        DRILL,          // დრელები / სახრახნისები
        ANGLE_GRINDER,  // კუთხსახეხები (ბალგარკები)
        ROTARY_HAMMER,  // პერფორატორები
        IMPACT_WRENCH,  // დამრტყმელი ქანჩსახრახნები
        SAW,// ხერხები
        NAIL_GUN,
        BATTERY,
        OTHER           // სხვადასხვა (აქსესუარები, ჩანთები და ა.შ.)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // მაგ: "Milwaukee M18 Fuel Hammer Drill"

    @Column(unique = true)
    private String sku; // უნიკალური კოდი, მაგ: "2904-20"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    private Integer stockQuantity; // რაოდენობა მარაგში

    // სპეციფიური მახასიათებლები
    private String voltage; // "M12", "M18", "MX FUEL"

    private Boolean isBrushless; // Fuel ტექნოლოგია

    private Boolean isToolOnly; // true = ელემენტის გარეშე, false = კიტი (Kit)

    private String imageUrl; // სურათის ლინკი

    // 2. ვამატებთ ახალ ველს ბაზისთვის. STRING ნიშნავს, რომ ბაზაში სიტყვებად ჩაიწერება (მაგ: "DRILL") და არა ციფრებად (0, 1)
    @Enumerated(EnumType.STRING)
    private Category category;


    // --- ქვემოთ დამატებულია ხელით დაწერილი Getters და Setters დაზღვევისთვის ---
    // ეს ხსნის ყველა "Cannot resolve method" ტიპის ერორს

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    // 3. კატეგორიის Getter და Setter
    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}

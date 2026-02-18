package ge.toolmasters.store.entity;

import jakarta.persistence.*;
import lombok.Data; // თუ წითლად ანთებს, მითხარი

@Entity
@Data // ლომბოკი ავტომატურად ქმნის Getters, Setters და Constructor-ს
@Table(name = "products")
public class Product {

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
}

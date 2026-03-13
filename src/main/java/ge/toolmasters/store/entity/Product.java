package ge.toolmasters.store.entity;

import jakarta.persistence.*;
import lombok.Data;

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
        SAW,            // ხერხები
        NAIL_GUN,
        BATTERY,
        MEASURING_TOOLS,
        SAFETY_GEAR,
        LIGHTING,
        TOOL_ACCESSORIES,
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

    // 🌟 დამატებული ველი: ფასდაკლების პროცენტი (აქციისთვის)
    private Integer discountPercentage;

    // 2. ვამატებთ ახალ ველს ბაზისთვის. STRING ნიშნავს, რომ ბაზაში სიტყვებად ჩაიწერება (მაგ: "DRILL") და არა ციფრებად (0, 1)
    @Enumerated(EnumType.STRING)
    private Category category;


    @Column(name = "has_warranty", columnDefinition = "boolean default false")
    private boolean hasWarranty;


    // --- 🚨 ახალი: კომპლექტაციის ველები 🚨 ---
    @Column(name = "has_battery")
    private Boolean hasBattery = false;

    @Column(name = "has_charger")
    private Boolean hasCharger = false;

    @Column(name = "has_case")
    private Boolean hasCase = false;


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

    // 🌟 აქციის პროცენტის Getter და Setter
    public Integer getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(Integer discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    // --- 🚨 ახალი: კომპლექტაციის Getters და Setters 🚨 ---
    public Boolean getHasBattery() {
        return hasBattery;
    }

    public void setHasBattery(Boolean hasBattery) {
        this.hasBattery = hasBattery;
    }

    public Boolean getHasCharger() {
        return hasCharger;
    }

    public void setHasCharger(Boolean hasCharger) {
        this.hasCharger = hasCharger;
    }

    public Boolean getHasCase() {
        return hasCase;
    }

    public void setHasCase(Boolean hasCase) {
        this.hasCase = hasCase;
    }

    public boolean getHasWarranty() {
        return hasWarranty;
    }

    public void setHasWarranty(boolean hasWarranty) {
        this.hasWarranty = hasWarranty;
    }


    // 🌟 ჭკვიანი მეთოდი, რომელიც ავტომატურად ითვლის ფასდაკლებულ თანხას!
    // (გამოიძახება HTML-დან: ${product.getDiscountedPrice()})
    public Double getDiscountedPrice() {
        if (discountPercentage == null || discountPercentage <= 0) {
            return price;
        }
        return price - (price * discountPercentage / 100.0);
    }
}

package ge.toolmasters.store.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders") // "order" დაცული სიტყვაა SQL-ში, ამიტომ "orders" ჯობია
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // მყიდველის ინფო
    private String customerName;
    private String customerPhone;
    private String customerEmail; // 🌟 ახალი: ელ-ფოსტის ველი ინვოისებისთვის
    private String customerAddress;

    // შეკვეთის დეტალები
    private Double totalAmount;
    private LocalDateTime orderDate;
    private String status; // მაგ: "ახალი შეკვეთა 🆕", "გადახდილია 💳"

    @Column(columnDefinition = "TEXT")
    private String itemsDescription; // მაგ: "Drill x1, Battery x2"

    // კონსტრუქტორი, რომ თარიღი ავტომატურად ჩაიწეროს
    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
        // თუ სტატუსი ცარიელია (მაგ: ბანკისგან არ მოუნიჭებია), მაშინ ვწერთ "ახალს"
        if (this.status == null) {
            this.status = "ახალი შეკვეთა 🆕";
        }
    }
}

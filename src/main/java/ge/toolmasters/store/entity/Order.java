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
    private String customerAddress;

    // შეკვეთის დეტალები
    private Double totalAmount;
    private LocalDateTime orderDate;
    private String status; // მაგ: "NEW", "SHIPPED", "DELIVERED"

    // აქ შეგვიძლია შევინახოთ ნივთების სია ტექსტად (მარტივი გზა)
    // ან ცალკე ცხრილში (რთული გზა). დამწყებისთვის ტექსტიც საკმარისია.
    @Column(columnDefinition = "TEXT")
    private String itemsDescription; // მაგ: "Drill x1, Battery x2"

    // კონსტრუქტორი, რომ თარიღი ავტომატურად ჩაიწეროს
    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
        status = "NEW";
    }
}

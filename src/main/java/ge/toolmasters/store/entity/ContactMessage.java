package ge.toolmasters.store.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "contact_messages")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String senderContact; // ტელეფონი ან მეილი

    @Column(columnDefinition = "TEXT", nullable = false)
    private String messageContent;

    private LocalDateTime receivedAt = LocalDateTime.now();

    private boolean isRead = false; // წაკითხულია თუ არა ადმინის მიერ
}

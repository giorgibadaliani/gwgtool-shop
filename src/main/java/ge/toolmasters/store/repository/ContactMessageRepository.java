package ge.toolmasters.store.repository;

import ge.toolmasters.store.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findAllByOrderByReceivedAtDesc(); // ახლები პირველ რიგში
}

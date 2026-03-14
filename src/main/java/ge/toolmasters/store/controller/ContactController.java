package ge.toolmasters.store.controller;

import ge.toolmasters.store.entity.ContactMessage;
import ge.toolmasters.store.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    @Autowired
    private ContactMessageRepository contactRepository;

    // ეს უკვე გქონდა ალბათ, მაგრამ ყოველი შემთხვევისთვის
    @GetMapping("/contact")
    public String showContactPage() {
        return "contact";
    }

    // 1. მესიჯის მიღება კლიენტისგან
    @PostMapping("/contact/submit")
    public String submitMessage(ContactMessage message, RedirectAttributes redirectAttributes) {
        contactRepository.save(message);
        redirectAttributes.addFlashAttribute("successMessage", "შეტყობინება წარმატებით გაიგზავნა! ჩვენ მალე დაგიკავშირდებით.");
        return "redirect:/contact";
    }

    // 2. ადმინ პანელის გვერდი მესიჯების სანახავად
    @GetMapping("/admin/messages")
    public String viewMessages(Model model) {
        model.addAttribute("messages", contactRepository.findAllByOrderByReceivedAtDesc());
        return "messages_admin"; // ამ HTML-ს შემდეგ ნაბიჯზე დაგიწერ!
    }

    // 3. შეტყობინების წაკითხულად მონიშვნა
    @PostMapping("/admin/messages/{id}/read")
    public String markAsRead(@PathVariable Long id) {
        contactRepository.findById(id).ifPresent(msg -> {
            msg.setRead(true);
            contactRepository.save(msg);
        });
        return "redirect:/admin/messages";
    }

    // 4. შეტყობინების წაშლა
    @PostMapping("/admin/messages/{id}/delete")
    public String deleteMessage(@PathVariable Long id) {
        contactRepository.deleteById(id);
        return "redirect:/admin/messages";
    }


}

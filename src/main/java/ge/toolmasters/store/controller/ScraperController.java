package ge.toolmasters.store.controller;

import ge.toolmasters.store.service.MilwaukeeScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScraperController {

    @Autowired
    private MilwaukeeScraperService scraperService;

    @GetMapping("/api/scrape-tools")
    public String runScraper() {
        // ⚠️ აქ შეცვალე გზა და მიუთითე შენი RealBase1.xlsx ფაილის ლოკაცია სისტემაში
        String filePath = "C:\\Users\\gbada\\OneDrive\\Documents";

        // ვუშვებთ ცალკე Thread-ში, რათა ბრაუზერმა Timeout არ მოგვცეს
        new Thread(() -> scraperService.scrapeFromExcel(filePath)).start();

        return "სკრეპინგი დაიწყო! გახსენი IDE-ს კონსოლი (Console) და დააკვირდი პროცესს.";
    }
}

package ge.toolmasters.store;

import ge.toolmasters.store.service.MilwaukeeScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StoreApplication {

	@Autowired
	private MilwaukeeScraperService scraperService;

	public static void main(String[] args) {
		SpringApplication.run(StoreApplication.class, args);
	}

	@Bean
	public CommandLineRunner runScraper() {
		return args -> {
			// ⚠️ ჩაწერე Excel-ის ფაილის ზუსტი მისამართი:
			String filePath = "C:\\Projects\\store\\RealBase1.xlsx";
			scraperService.scrapeFromExcel(filePath);
		};
	}
}

package ge.toolmasters.store.service;

import ge.toolmasters.store.entity.Product;
import ge.toolmasters.store.repository.ProductRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("import")
public class MilwaukeeDataImporter implements CommandLineRunner {

    private final ProductRepository productRepository;

    public MilwaukeeDataImporter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== MILWAUKEE IMPORTER STARTED ===");

        List<Product> products = productRepository.findAll();
        System.out.println("=== DB-ში პროდუქტების რაოდენობა: " + products.size() + " ===");

        int updated = 0;
        int notFound = 0;

        for (Product product : products) {
            String sku = product.getSku();
            if (sku == null || sku.isBlank()) {
                System.out.println("⚠️  SKU არ არის: " + product.getName());
                continue;
            }

            try {
                System.out.println("🔍 ვეძებ SKU: " + sku);

                // Search URL — redirect-ს მიჰყვება პროდუქტის გვერდზე
                String searchUrl = "https://www.milwaukeetool.eu/en-eu/?s=" + sku;


                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                        .referrer("https://www.milwaukeetool.eu")
                        .followRedirects(true)
                        .timeout(15000)
                        .get();

                // redirect-ის შემდეგ მიმდინარე URL
                String currentUrl = doc.location();
                System.out.println("   → " + currentUrl);

                // თუ search გვერდზე დარჩა (ვერ ვიპოვეთ)
                if (currentUrl.contains("/search") || currentUrl.contains("?q=")) {
                    // search results-დან პირველი პროდუქტი ამოვიღოთ
                    Element firstResult = doc.selectFirst("a[href*='/en-eu/']");
                    if (firstResult != null) {
                        String href = firstResult.absUrl("href");
                        if (!href.contains("/search") && !href.isEmpty()) {
                            doc = Jsoup.connect(href)
                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                    .timeout(15000)
                                    .get();
                            currentUrl = href;
                        } else {
                            System.out.println("❌ ვერ ვიპოვე: " + sku);
                            notFound++;
                            Thread.sleep(1000);
                            continue;
                        }
                    } else {
                        System.out.println("❌ ვერ ვიპოვე: " + sku);
                        notFound++;
                        Thread.sleep(1000);
                        continue;
                    }
                }

                // პროდუქტის გვერდიდან მახასიათებლები
                StringBuilder description = new StringBuilder();

                // Features სია
                Elements features = doc.select(
                        "ul.product-features li, " +
                                ".features-list li, " +
                                "[class*='feature'] li, " +
                                ".pdp-features li, " +
                                ".product-highlights li"
                );

                if (!features.isEmpty()) {
                    description.append("მახასიათებლები:\n");
                    for (Element f : features) {
                        String text = f.text().trim();
                        if (!text.isEmpty() && text.length() > 3) {
                            description.append("• ").append(text).append("\n");
                        }
                    }
                }

                // Specification ცხრილი
                Elements specRows = doc.select(
                        ".specification-table tr, " +
                                "[class*='spec'] tr, " +
                                "table.specs tr, " +
                                ".pdp-specs tr"
                );

                if (!specRows.isEmpty()) {
                    description.append("\nსპეციფიკაცია:\n");
                    for (Element row : specRows) {
                        Elements cells = row.select("td");
                        if (cells.size() >= 2) {
                            String key = cells.get(0).text().trim();
                            String val = cells.get(1).text().trim();
                            if (!key.isEmpty() && !val.isEmpty()) {
                                description.append(key).append(": ").append(val).append("\n");
                            }
                        }
                    }
                }

                // თუ ვერ ამოვიღეთ — h1 სულ მცირე
                if (description.length() < 10) {
                    Element h1 = doc.selectFirst("h1");
                    if (h1 != null) {
                        description.append(h1.text().trim());
                    }
                }

                if (description.length() > 5) {
                    product.setDescription(description.toString().trim());
                    productRepository.save(product);
                    System.out.println("✅ განახლდა: " + product.getName());
                    updated++;
                } else {
                    System.out.println("⚠️  description ვერ ამოვიღე: " + product.getName());
                    notFound++;
                }

                Thread.sleep(1500);

            } catch (Exception e) {
                System.out.println("❌ შეცდომა SKU=" + sku + ": " + e.getMessage());
                notFound++;
                Thread.sleep(1000);
            }
        }

        System.out.println("\n=============================");
        System.out.println("✅ განახლდა: " + updated + " პროდუქტი");
        System.out.println("❌ ვერ მოიძებნა: " + notFound + " პროდუქტი");
        System.out.println("=============================");
    }
}

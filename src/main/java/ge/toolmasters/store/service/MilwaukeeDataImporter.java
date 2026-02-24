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
@Profile("milwaukee-import")
public class MilwaukeeDataImporter implements CommandLineRunner {

    private final ProductRepository productRepository;

    public MilwaukeeDataImporter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== MILWAUKEE IMPORTER STARTED (SMART V3) ===");

        List<Product> products = productRepository.findAll();
        System.out.println("=== DB-ში პროდუქტების რაოდენობა: " + products.size() + " ===");

        int updated = 0;
        int notFound = 0;

        for (Product product : products) {
            String sku = product.getSku();
            if (sku == null || sku.isBlank()) continue;

            try {
                System.out.println("🔍 ვეძებ SKU: " + sku);

                // ოფიციალური ძებნის გვერდი
                String searchUrl = "https://www.milwaukeetool.eu/support/search-results/?q=" + sku;

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .referrer("https://www.google.com/")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .timeout(30000)
                        .followRedirects(true)
                        .get();

                String currentUrl = doc.location();
                String productUrl = null;

                // 1. თუ პირდაპირ გადამისამართდა (იდეალური ვარიანტი)
                if (!currentUrl.contains("search-results") && !currentUrl.contains("?q=")) {
                    productUrl = currentUrl;
                }
                // 2. თუ ძებნის შედეგებში ვართ, უნდა ვიპოვოთ სწორი ლინკი
                else {
                    // ვეძებთ ლინკებს, რომლებიც არ არის "hand-tools" ან "power-tools" კატეგორიები
                    // და სასურველია შეიცავდეს SKU-ს ან ციფრებს
                    Elements results = doc.select("a.product-card, .search-result a, a[href*='/en-eu/']");

                    for (Element link : results) {
                        String href = link.absUrl("href");
                        // ფილტრაცია: არ გვინდა ზოგადი კატეგორიები
                        if (href.contains("/en-eu/") &&
                                !href.endsWith("/hand-tools/") &&
                                !href.endsWith("/power-tools/") &&
                                !href.endsWith("/accessories/") &&
                                !href.contains("search-results")) {

                            productUrl = href;
                            break; // ვიპოვეთ პირველივე ვალიდური პროდუქტი
                        }
                    }
                }

                if (productUrl == null) {
                    System.out.println("❌ ვერ ვიპოვე შესაბამისი პროდუქტის ლინკი: " + sku);
                    notFound++;
                    Thread.sleep(1000);
                    continue;
                }

                System.out.println("   → გადავდივარ: " + productUrl);

                // შევდივართ პროდუქტის გვერდზე
                doc = Jsoup.connect(productUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(30000)
                        .get();

                StringBuilder description = new StringBuilder();

                // 1. Features (მახასიათებლები)
                Elements features = doc.select("ul.product-features li, .features-list li, .pdp-features li");
                if (!features.isEmpty()) {
                    description.append("მახასიათებლები:\n");
                    for (Element f : features) {
                        String text = f.text().trim();
                        if (!text.isEmpty() && text.length() > 5) {
                            description.append("• ").append(text).append("\n");
                        }
                    }
                }

                // 2. Specifications (ტექნიკური მონაცემები)
                Elements specRows = doc.select("table.table-striped tr, table.specifications tr, .tech-specs tr, .specification-table tr");
                if (!specRows.isEmpty()) {
                    if (description.length() > 0) description.append("\n");
                    description.append("სპეციფიკაცია:\n");

                    for (Element row : specRows) {
                        Elements cells = row.select("th, td");
                        if (cells.size() >= 2) {
                            String key = cells.get(0).text().trim();
                            String val = cells.get(1).text().trim();
                            // ფილტრაცია: არ გვინდა "Specification" სათაური და ცარიელი ველები
                            if (!key.isEmpty() && !val.isEmpty() && !key.equalsIgnoreCase("Specification")) {
                                description.append(key).append(": ").append(val).append("\n");
                            }
                        }
                    }
                }

                // 3. Fallback: H1-ის წამოღება, მაგრამ დაცვით!
                if (description.length() < 10) {
                    Element h1 = doc.selectFirst("h1");
                    if (h1 != null) {
                        String h1Text = h1.text().trim();
                        // ⛔️ მკაცრი აკრძალვა: არ დავწეროთ კატეგორიის სახელები!
                        if (!h1Text.equalsIgnoreCase("Hand Tools") &&
                                !h1Text.equalsIgnoreCase("Power Tools") &&
                                !h1Text.equalsIgnoreCase("Accessories") &&
                                !h1Text.contains("Search Results")) {
                            description.append(h1Text);
                        }
                    }
                }

                if (description.length() > 5) {
                    // ეტაპობრივი განახლება, რომ სესია არ გაწყდეს
                    Product prodToUpdate = productRepository.findById(product.getId()).orElse(null);
                    if (prodToUpdate != null) {
                        prodToUpdate.setDescription(description.toString().trim());
                        productRepository.save(prodToUpdate);
                        System.out.println("✅ განახლდა: " + prodToUpdate.getName());
                        updated++;
                    }
                } else {
                    System.out.println("⚠️ ინფორმაცია ვერ ამოვიღე: " + sku);
                    notFound++;
                }

                Thread.sleep(1500); // 1.5 წამი პაუზა

            } catch (Exception e) {
                System.out.println("❌ შეცდომა SKU=" + sku + ": " + e.getMessage());
                notFound++;
            }
        }

        System.out.println("\n=============================");
        System.out.println("✅ სულ განახლდა: " + updated + " პროდუქტი");
        System.out.println("❌ ვერ მოიძებნა ან ერორი: " + notFound + " პროდუქტი");
        System.out.println("=============================");
    }
}

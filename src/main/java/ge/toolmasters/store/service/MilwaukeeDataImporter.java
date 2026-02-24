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
        System.out.println("=== MILWAUKEE IMPORTER STARTED ===");

        List<Product> products = productRepository.findAll();
        System.out.println("=== DB-ში პროდუქტების რაოდენობა: " + products.size() + " ===");

        int updated = 0;
        int notFound = 0;

        for (Product product : products) {
            String sku = product.getSku();
            if (sku == null || sku.isBlank()) {
                continue;
            }

            try {
                System.out.println("🔍 ვეძებ SKU: " + sku);

                String searchUrl = "https://www.milwaukeetool.eu/support/search-results/?q=" + sku;

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .referrer("https://www.google.com")
                        .followRedirects(true)
                        .timeout(20000)
                        .get();

                String currentUrl = doc.location();

                // 1. გადამისამართება ძებნის შედეგებიდან
                if (currentUrl.contains("search-results") || currentUrl.contains("?q=")) {
                    Element firstResult = doc.selectFirst("a.product-card, .search-result a, a[href*='/en-eu/'][href*='/m18-'], a[href*='/en-eu/'][href*='/m12-'], a[href*='/en-eu/'][href*='/hand-tools/']");

                    if (firstResult != null) {
                        String href = firstResult.absUrl("href");
                        doc = Jsoup.connect(href)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                .timeout(20000)
                                .get();
                    } else {
                        System.out.println("❌ ვერ ვიპოვე ძებნის შედეგებში: " + sku);
                        notFound++;
                        Thread.sleep(1000);
                        continue;
                    }
                }

                StringBuilder description = new StringBuilder();

                // 2. ამოვიღოთ მახასიათებლების სია (ბულეტები)
                Elements features = doc.select(
                        "ul.product-features li, " +
                                ".features-list li, " +
                                "ul.list-bullet li, " +
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

                // 3. ამოვიღოთ დეტალური ტექნიკური მონაცემები (RPM, ნიუტონმეტრი, ძაბვა და ა.შ.)
                // Milwaukee ძირითადად იყენებს table.table-striped կლასს თავისი სპეციფიკაციებისთვის
                Elements specRows = doc.select(
                        "table.table-striped tr, " +
                                "table.specifications tr, " +
                                ".tech-specs tr, " +
                                ".specification-table tr, " +
                                ".specs-table tr, " +
                                "table.table tr" // ყველაზე ზოგადი მაინც დავიჭიროთ
                );

                if (!specRows.isEmpty()) {
                    // თუ უკვე გვაქვს მახასიათებლები, ცოტა დავაშოროთ
                    if (description.length() > 0) {
                        description.append("\n");
                    }
                    description.append("დამატებითი მონაცემები:\n");

                    for (Element row : specRows) {
                        // ვეძებთ ორ სვეტს: 1-ლი არის პარამეტრის სახელი (მაგ. RPM), მე-2 მნიშვნელობა (მაგ. 2000)
                        Elements cells = row.select("th, td");
                        if (cells.size() == 2) {
                            String key = cells.get(0).text().trim();
                            String val = cells.get(1).text().trim();

                            // ვიზღვევთ თავს, რომ ცარიელი ან სათაურის ველები არ ჩავწეროთ
                            if (!key.isEmpty() && !val.isEmpty() && !key.equalsIgnoreCase("Specification")) {
                                description.append(key).append(": ").append(val).append("\n");
                            }
                        }
                    }
                }

                // 4. თუ საერთოდ ვერაფერი იპოვა, აღწერა მაინც ამოვიღოთ
                if (description.length() < 10) {
                    Element h1 = doc.selectFirst("h1");
                    Element prodDesc = doc.selectFirst(".product-description, .description-text");

                    if (prodDesc != null && !prodDesc.text().isEmpty()) {
                        description.append(prodDesc.text().trim());
                    } else if (h1 != null) {
                        description.append(h1.text().trim());
                    }
                }

                // 5. ბაზაში შენახვა
                if (description.length() > 5) {
                    product.setDescription(description.toString().trim());
                    productRepository.save(product);
                    System.out.println("✅ დაემატა სპეციფიკაციები: " + product.getName());
                    updated++;
                } else {
                    System.out.println("⚠️ description ვერ ამოვიღე: " + sku);
                    notFound++;
                }

                // დაყოვნება, რომ IP არ დაგვიბლოკოს
                Thread.sleep(1500);

            } catch (Exception e) {
                System.out.println("❌ შეცდომა SKU=" + sku + ": " + e.getMessage());
                notFound++;
                Thread.sleep(1000);
            }
        }

        System.out.println("\n=============================");
        System.out.println("✅ წარმატებით განახლდა: " + updated + " პროდუქტი");
        System.out.println("❌ ვერ მოიძებნა ან ერორი: " + notFound + " პროდუქტი");
        System.out.println("=============================");
    }
}

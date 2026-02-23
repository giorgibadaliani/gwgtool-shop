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
                System.out.println("⚠️  SKU არ არის: " + product.getName());
                continue;
            }

            try {
                System.out.println("🔍 ვეძებ SKU: " + sku);

                // ახალი, უფრო საიმედო საძიებო ლინკი
                String searchUrl = "https://www.milwaukeetool.eu/support/search-results/?q=" + sku;

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                        .referrer("https://www.google.com")
                        .followRedirects(true)
                        .timeout(20000) // დრო გავზარდეთ 20 წამამდე
                        .get();

                String currentUrl = doc.location();
                System.out.println("   → მიმდინარე ლინკი: " + currentUrl);

                // თუ ისევ ძებნის გვერდზე ვართ, ესეიგი ავტომატური გადამისამართება არ მოხდა
                if (currentUrl.contains("search-results") || currentUrl.contains("?q=")) {

                    // ვეძებთ პროდუქტის ბარათის ლინკს საძიებო შედეგებში
                    Element firstResult = doc.selectFirst("a.product-card, .search-result a, a[href*='/en-eu/'][href*='/m18-'], a[href*='/en-eu/'][href*='/m12-'], a[href*='/en-eu/'][href*='/hand-tools/']");

                    if (firstResult != null) {
                        String href = firstResult.absUrl("href");
                        System.out.println("   → გადავდივარ ნაპოვნ პროდუქტზე: " + href);

                        doc = Jsoup.connect(href)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                                .timeout(20000)
                                .get();
                    } else {
                        System.out.println("❌ ვერ ვიპოვე პროდუქტის ლინკი ძებნის შედეგებში: " + sku);
                        notFound++;
                        Thread.sleep(1000);
                        continue;
                    }
                }

                // პროდუქტის გვერდიდან მახასიათებლების ამოღება
                StringBuilder description = new StringBuilder();

                // 1. Features სია (განახლებული სელექტორები)
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

                // 2. Specification ცხრილი (განახლებული სელექტორები)
                Elements specRows = doc.select(
                        ".specification-table tr, " +
                                ".table-striped tr, " +
                                "table.specs tr, " +
                                ".pdp-specs tr"
                );

                if (!specRows.isEmpty()) {
                    description.append("\nსპეციფიკაცია:\n");
                    for (Element row : specRows) {
                        Elements cells = row.select("td, th"); // th-ს დამატება, რადგან ხანდახან label <th>-შია
                        if (cells.size() >= 2) {
                            String key = cells.get(0).text().trim();
                            String val = cells.get(1).text().trim();
                            if (!key.isEmpty() && !val.isEmpty()) {
                                description.append(key).append(": ").append(val).append("\n");
                            }
                        }
                    }
                }

                // 3. თუ ვერ ამოვიღეთ ვერაფერი — .product-description ან h1 მაინც
                if (description.length() < 10) {
                    Element h1 = doc.selectFirst("h1");
                    Element prodDesc = doc.selectFirst(".product-description, .description-text");

                    if (prodDesc != null && !prodDesc.text().isEmpty()) {
                        description.append(prodDesc.text().trim());
                    } else if (h1 != null) {
                        description.append(h1.text().trim());
                    }
                }

                if (description.length() > 5) {
                    product.setDescription(description.toString().trim());
                    productRepository.save(product);
                    System.out.println("✅ განახლდა: " + product.getName());
                    updated++;
                } else {
                    System.out.println("⚠️  description ვერ ამოვიღე: " + product.getName() + " (SKU: " + sku + ")");
                    notFound++;
                }

                // Milwaukee-ს დაცვა რომ არ დაგვბლოკოს, ცოტა მეტს ველოდებით ყოველ რექვესთზე
                Thread.sleep(2000);

            } catch (Exception e) {
                System.out.println("❌ შეცდომა SKU=" + sku + ": " + e.getMessage());
                notFound++;
                Thread.sleep(1000);
            }
        }

        System.out.println("\n=============================");
        System.out.println("✅ სულ განახლდა: " + updated + " პროდუქტი");
        System.out.println("❌ ვერ მოიძებნა ან ერორი: " + notFound + " პროდუქტი");
        System.out.println("=============================");
    }
}

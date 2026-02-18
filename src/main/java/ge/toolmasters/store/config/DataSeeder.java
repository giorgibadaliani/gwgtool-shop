package ge.toolmasters.store.config;

import ge.toolmasters.store.entity.Product;
import ge.toolmasters.store.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    // ვალუტის კურსი (ევრო -> ლარი)
    private final double EURO_RATE = 3.0;

    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. ჯერ ვასუფთავებთ ბაზას, რომ დუბლიკატები და ძველი ნაგავი არ დარჩეს (სურვილისამებრ)
        // თუ გინდა რომ ძველი მონაცემები დარჩეს, ეს ხაზი დააკომენტარე:
        if (productRepository.count() == 0) {
            System.out.println("⏳ ბაზა ცარიელია, ვიწყებ შევსებას...");
        } else {
            System.out.println("⚠️ ბაზაში უკვე არის მონაცემები. ვცდილობ მხოლოდ ახლების დამატებას...");
        }

        System.out.println("⏳ ვიწყებ Excel-იდან მონაცემების წაკითხვას...");

        // --- ნაბიჯი 1: RealBase-ის წაკითხვა (რაოდენობები) ---
        Map<String, Integer> realStockMap = new HashMap<>();

        try (InputStream realStream = new ClassPathResource("RealBase.xlsx").getInputStream();
             Workbook realWorkbook = new XSSFWorkbook(realStream)) {

            Sheet sheet = realWorkbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // სათაური

                // A სვეტი = SKU, B სვეტი = რაოდენობა
                String sku = getCellValue(row.getCell(0));
                String qtyStr = getCellValue(row.getCell(1));

                if (!sku.isEmpty() && !qtyStr.isEmpty()) {
                    // SKU-ს გასუფთავება
                    sku = sku.trim();

                    try {
                        int quantity = (int) Double.parseDouble(qtyStr);
                        realStockMap.put(sku, quantity);
                    } catch (NumberFormatException e) {
                        System.out.println("⚠️ შეცდომა რაოდენობის წაკითხვისას SKU: " + sku);
                    }
                }
            }
        }
        System.out.println("✅ RealBase წაკითხულია! ნაპოვნია " + realStockMap.size() + " ნივთი.");


        // --- ნაბიჯი 2: MainBase-ის წაკითხვა და შერწყმა ---
        int addedCount = 0;
        int skippedCount = 0;

        try (InputStream mainStream = new ClassPathResource("MainBase.xlsx").getInputStream();
             Workbook mainWorkbook = new XSSFWorkbook(mainStream)) {

            Sheet sheet = mainWorkbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // სათაური

                // მონაცემების წაკითხვა
                String mainSku = getCellValue(row.getCell(0)); // A - SKU
                String name = getCellValue(row.getCell(1));    // B - სახელი
                String priceEurStr = getCellValue(row.getCell(2)); // C - ფასი

                // სურათის ლინკის ამოღება (E სვეტი - ინდექსი 4)
                Cell imageCell = row.getCell(4);
                String imageUrl = "";
                if (imageCell != null) {
                    if (imageCell.getHyperlink() != null) {
                        imageUrl = imageCell.getHyperlink().getAddress();
                    } else {
                        imageUrl = getCellValue(imageCell);
                    }
                }

                // ალგორითმი:
                // 1. თუ ეს SKU არის RealBase-ში (ანუ გვაქვს მარაგში)
                // 2. და თუ ეს SKU ჯერ არ არის ბაზაში (დუბლიკატის თავიდან აცილება)
                if (realStockMap.containsKey(mainSku)) {

                    // ვამოწმებთ, ბაზაში ხომ არ არის უკვე?
                    if (productRepository.findBySku(mainSku).isPresent()) {
                        System.out.println("⚠️ დუბლიკატი: " + mainSku + " უკვე ბაზაშია. ვატარებ.");
                        skippedCount++;
                        continue;
                    }

                    Product product = new Product();
                    product.setName(name);
                    product.setSku(mainSku);
                    product.setImageUrl(imageUrl);
                    product.setStockQuantity(realStockMap.get(mainSku));

                    // ფასის კონვერტაცია
                    if (!priceEurStr.isEmpty()) {
                        try {
                            double priceEur = Double.parseDouble(priceEurStr);
                            product.setPrice(priceEur * EURO_RATE);
                        } catch (NumberFormatException e) {
                            product.setPrice(0.0);
                        }
                    } else {
                        product.setPrice(0.0);
                    }

                    productRepository.save(product);
                    addedCount++;
                }
            }
        }

        System.out.println("🎉 დასრულდა! დაემატა: " + addedCount + ", გამოტოვებულია (დუბლიკატი): " + skippedCount);
    }

    // დამხმარე მეთოდი (განახლებული, E9 პრობლემის გარეშე)
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // რიცხვს ვკითხულობთ როგორც მთელს (long), რომ არ დაწეროს 4.93E9
                long longVal = (long) cell.getNumericCellValue();
                return String.valueOf(longVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }
}

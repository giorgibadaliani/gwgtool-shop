package ge.toolmasters.store.service;

import ge.toolmasters.store.entity.Product;
import ge.toolmasters.store.entity.ProductCharacteristic;
import ge.toolmasters.store.repository.ProductCharacteristicRepository;
import ge.toolmasters.store.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MilwaukeeScraperService {

    @Autowired
    private ProductCharacteristicRepository characteristicRepo;

    @Autowired
    private ProductRepository productRepository;

    public void scrapeFromExcel(String excelFilePath) {
        System.out.println("🚀 ვიწყებთ მახასიათებლების გენერირებას...");
        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // ჰედერის გამოტოვება

                Cell cell = row.getCell(0);
                if (cell == null) continue;

                String sku = getSkuFromCell(cell);
                if (sku != null && !sku.isEmpty()) {
                    scrapeMilwaukeeProduct(sku);
                }
            }
            System.out.println("✅ მახასიათებლების გენერირება წარმატებით დასრულდა!");

        } catch (Exception e) {
            System.err.println("❌ პრობლემა Excel ფაილის წაკითხვისას: " + e.getMessage());
        }
    }

    private String getSkuFromCell(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC) {
            // BigDecimal გვეხმარება, რომ E+09 ფორმატი ჩვეულებრივ ციფრად ვაქციოთ
            return new java.math.BigDecimal(cell.getNumericCellValue()).toPlainString();
        } else if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            // თუ ტექსტად წერია "4.93347E+09", ესეც გავასწოროთ
            if (value.toUpperCase().contains("E+")) {
                try {
                    return new java.math.BigDecimal(value).toPlainString();
                } catch (Exception e) {
                    return value;
                }
            }
            return value;
        }
        return "";
    }


    private void scrapeMilwaukeeProduct(String sku) {
        try {
            System.out.println("   → ვეძებ მახასიათებლებს ლოკალურად SKU-სთვის: " + sku);

            // 1. მოვძებნოთ ეს პროდუქტი ჩვენს ბაზაში (Optional-ის გამოყენებით)
            Optional<Product> productOpt = productRepository.findBySku(sku);

            if (productOpt.isEmpty()) {
                System.out.println("  ❌ პროდუქტი ვერ მოიძებნა ბაზაში: " + sku);
                return;
            }

            // ამოვიღოთ პროდუქტი Optional "ყუთიდან"
            Product product = productOpt.get();

            List<ProductCharacteristic> newCharacteristics = new ArrayList<>();
            // ვიღებთ სახელს და ვაქცევთ დიდ ასოებად შედარებისთვის
            String name = product.getName().toUpperCase();

            // --- 2. სახელიდან მახასიათებლების დაგენერირება ---

            // პლატფორმა / ვოლტაჟი
            if (name.contains("M18")) {
                addCharacteristic(newCharacteristics, sku, "Platform", "M18™");
                addCharacteristic(newCharacteristics, sku, "Voltage (V)", "18");
            } else if (name.contains("M12")) {
                addCharacteristic(newCharacteristics, sku, "Platform", "M12™");
                addCharacteristic(newCharacteristics, sku, "Voltage (V)", "12");
            } else if (name.contains("MX FUEL")) {
                addCharacteristic(newCharacteristics, sku, "Platform", "MX FUEL™");
            }

            // ძრავის ტიპი
            if (name.contains("FUEL") || name.contains("BRUSHLESS") || name.contains("BL")) {
                addCharacteristic(newCharacteristics, sku, "Motor Type", "Brushless (Without Brushes)");
            } else {
                addCharacteristic(newCharacteristics, sku, "Motor Type", "Brushed");
            }

            // სისტემა (One-Key)
            if (name.contains("ONE-KEY") || name.contains("ONE KEY") || name.contains("ONEKEY")) {
                addCharacteristic(newCharacteristics, sku, "Technology", "ONE-KEY™ Compatible");
            }

            // პროდუქტის კატეგორია / ტიპი
            if (name.contains("DRILL") || name.contains("DRIVER")) {
                addCharacteristic(newCharacteristics, sku, "Tool Type", "Drill / Driver");
            } else if (name.contains("IMPACT WRENCH")) {
                addCharacteristic(newCharacteristics, sku, "Tool Type", "Impact Wrench");
                if (name.contains("1/2")) addCharacteristic(newCharacteristics, sku, "Anvil Size", "1/2\" Square");
                if (name.contains("3/4")) addCharacteristic(newCharacteristics, sku, "Anvil Size", "3/4\" Square");
                if (name.contains("1\"") || name.contains("1 INCH")) addCharacteristic(newCharacteristics, sku, "Anvil Size", "1\" Square");
                if (name.contains("FRICTION RING")) addCharacteristic(newCharacteristics, sku, "Anvil Type", "Friction Ring");
                if (name.contains("PIN DETENT")) addCharacteristic(newCharacteristics, sku, "Anvil Type", "Pin Detent");
            } else if (name.contains("GRINDER")) {
                addCharacteristic(newCharacteristics, sku, "Tool Type", "Grinder");
                if (name.contains("PADDLE SWITCH")) addCharacteristic(newCharacteristics, sku, "Switch Type", "Paddle Switch (Non-Lockable)");
                if (name.contains("BRAKING")) addCharacteristic(newCharacteristics, sku, "Brake", "Rapid Stop Braking System");
                if (name.contains("115")) addCharacteristic(newCharacteristics, sku, "Disc Diameter (mm)", "115");
                if (name.contains("125")) addCharacteristic(newCharacteristics, sku, "Disc Diameter (mm)", "125");
                if (name.contains("230")) addCharacteristic(newCharacteristics, sku, "Disc Diameter (mm)", "230");
            } else if (name.contains("SAW")) {
                if (name.contains("CIRCULAR")) addCharacteristic(newCharacteristics, sku, "Tool Type", "Circular Saw");
                else if (name.contains("RECIP")) addCharacteristic(newCharacteristics, sku, "Tool Type", "Reciprocating Saw (Sawzall)");
                else if (name.contains("MITRE")) addCharacteristic(newCharacteristics, sku, "Tool Type", "Mitre Saw");
                else if (name.contains("BAND")) addCharacteristic(newCharacteristics, sku, "Tool Type", "Band Saw");
                else addCharacteristic(newCharacteristics, sku, "Tool Type", "Saw");
            } else if (name.contains("HAMMER")) {
                if (name.contains("ROTARY") || name.contains("SDS")) addCharacteristic(newCharacteristics, sku, "Tool Type", "Rotary Hammer");
                if (name.contains("SDS-PLUS") || name.contains("SDS PLUS")) addCharacteristic(newCharacteristics, sku, "Chuck Type", "SDS-Plus");
                if (name.contains("SDS-MAX") || name.contains("SDS MAX")) addCharacteristic(newCharacteristics, sku, "Chuck Type", "SDS-Max");
            } else if (name.contains("NAILER") || name.contains("STAPLER")) {
                addCharacteristic(newCharacteristics, sku, "Tool Type", "Nailer / Stapler");
            }

            // კომპლექტაცია
            if (name.contains("BARE") || name.contains("-0") || name.contains("TOOL ONLY")) {
                addCharacteristic(newCharacteristics, sku, "Supplied in", "Carton/No Kitbox");
                addCharacteristic(newCharacteristics, sku, "Batteries Supplied", "0");
                addCharacteristic(newCharacteristics, sku, "Charger Supplied", "No");
            } else if (name.contains("KIT") || name.contains("PACK") || name.matches(".*[1-9]X.*AH.*")) {
                addCharacteristic(newCharacteristics, sku, "Supplied in", "HD Box / Kitbox");
                addCharacteristic(newCharacteristics, sku, "Charger Supplied", "Yes");

                // ვცდილობთ ამოვიღოთ ბატარეის მოცულობა
                if (name.contains("5.0AH") || name.contains("5AH")) addCharacteristic(newCharacteristics, sku, "Battery Capacity", "5.0 Ah");
                else if (name.contains("4.0AH") || name.contains("4AH")) addCharacteristic(newCharacteristics, sku, "Battery Capacity", "4.0 Ah");
                else if (name.contains("2.0AH") || name.contains("2AH")) addCharacteristic(newCharacteristics, sku, "Battery Capacity", "2.0 Ah");
                else if (name.contains("8.0AH") || name.contains("8AH")) addCharacteristic(newCharacteristics, sku, "Battery Capacity", "8.0 Ah");
                else if (name.contains("12.0AH") || name.contains("12AH")) addCharacteristic(newCharacteristics, sku, "Battery Capacity", "12.0 Ah");
            }

            // Packout სისტემა
            if (name.contains("PACKOUT")) {
                addCharacteristic(newCharacteristics, sku, "System", "PACKOUT™ Modular Storage");
            }

            // 3. შევინახოთ თუ რაიმე ვიპოვეთ
            if (!newCharacteristics.isEmpty()) {
                saveCharacteristicsToDatabase(sku, newCharacteristics);
                System.out.println("  ✅ დაგენერირდა " + newCharacteristics.size() + " მახასიათებელი.");
            } else {
                System.out.println("  ⚠️ მახასიათებლების დაგენერირება ვერ მოხერხდა (არასტანდარტული სახელი).");
            }

        } catch (Exception e) {
            System.err.println("  ❌ ერორი SKU-ზე " + sku + ": " + e.getMessage());
        }
    }

    private void addCharacteristic(List<ProductCharacteristic> list, String sku, String key, String value) {
        for (ProductCharacteristic pc : list) {
            if (pc.getName().equals(key)) return; // უკვე გვაქვს, არ ვამატებთ
        }
        ProductCharacteristic pc = new ProductCharacteristic();
        pc.setSku(sku);
        pc.setName(key);
        pc.setValue(value);
        list.add(pc);
    }

    private void saveCharacteristicsToDatabase(String sku, List<ProductCharacteristic> characteristics) {
        // ჯერ ვშლით ძველ მახასიათებლებს ამ SKU-ზე, რომ არ გაორმაგდეს
        characteristicRepo.deleteBySku(sku);

        // ვინახავთ ახლებს
        characteristicRepo.saveAll(characteristics);
    }
}

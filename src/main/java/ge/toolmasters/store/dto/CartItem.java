package ge.toolmasters.store.dto;

import ge.toolmasters.store.entity.Product;
import lombok.Data;

@Data // Lombok ავტომატურად გაუკეთებს Getters/Setters
public class CartItem {
    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    // დამხმარე მეთოდი: ამ ნივთის ჯამური ფასი (ფასი * რაოდენობა)
    public double getTotalPrice() {
        return product.getPrice() * quantity;
    }
}

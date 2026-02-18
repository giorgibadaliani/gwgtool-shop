package ge.toolmasters.store.service;

import ge.toolmasters.store.dto.CartItem;
import ge.toolmasters.store.entity.Product;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;

@Service
@SessionScope // <--- ეს უმნიშვნელოვანესია!
// ეს ნიშნავს: თითოეულ მომხმარებელს აქვს თავისი პირადი კალათა.
// გიორგის კალათა არ აირევა ნიკას კალათაში.
public class CartService {

    private List<CartItem> items = new ArrayList<>();

    // კალათაში დამატება
    public void addToCart(Product product) {
        // ვამოწმებთ, უკვე ხომ არ დევს ეს ნივთი კალათაში
        for (CartItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + 1); // თუ დევს, რაოდენობას ვუმატებთ
                return;
            }
        }
        // თუ არ დევს, ვამატებთ ახალს (რაოდენობა 1)
        items.add(new CartItem(product, 1));
    }

    // კალათიდან წაშლა
    public void removeFromCart(Long productId) {
        items.removeIf(item -> item.getProduct().getId().equals(productId));
    }

    // ნივთების წამოღება
    public List<CartItem> getItems() {
        return items;
    }

    // კალათის გასუფთავება (ყიდვის მერე)
    public void clearCart() {
        items.clear();
    }

    // სრული ჯამური თანხა
    public double getTotalAmount() {
        return items.stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum();
    }
}

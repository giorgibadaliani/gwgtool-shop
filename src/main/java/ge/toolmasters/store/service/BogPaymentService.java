package ge.toolmasters.store.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class BogPaymentService {

    @Value("${bog.client.id}")
    private String clientId;

    @Value("${bog.secret.key}")
    private String secretKey;

    @Value("${bog.redirect.success.url}")
    private String successUrl;

    @Value("${bog.redirect.fail.url}")
    private String failUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 1. ტოკენის მიღება ბანკისგან
     */
    private String getAccessToken() {
        String tokenUrl = "https://oauth2.bog.ge/auth/realms/bog/protocol/openid-connect/token";

        String authCredentials = clientId + ":" + secretKey;
        String base64Auth = Base64.getEncoder().encodeToString(authCredentials.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + base64Auth); // შეცვლილია სტანდარტული Header-ით

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("access_token").asText();
        } catch (Exception e) {
            System.err.println("❌ ტოკენის აღება ვერ მოხერხდა: " + e.getMessage());
            return null;
        }
    }

    /**
     * 2. ბანკში შეკვეთის გაგზავნა
     */
    public String createOrder(Double totalAmount, String orderId) {
        String token = getAccessToken();
        if (token == null) return null;

        String orderUrl = "https://api.bog.ge/payments/v1/ecommerce/orders";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token); // შეცვლილია სტანდარტული Header-ით
        headers.set("Accept-Language", "ka");

        // ვიყენებთ ზოგად <String, Object> რუკებს, რომ Java არ დაიბნეს
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("callback_url", "https://gwgtools.ge/payment/callback");
        requestBody.put("external_order_id", orderId);

        // ფასის და ვალუტის ბლოკი
        Map<String, Object> purchaseUnits = new HashMap<>();
        purchaseUnits.put("currency", "GEL");
        // BOG ზოგჯერ თანხას String ფორმატში ითხოვს, რომ სიზუსტე არ დაიკარგოს
        purchaseUnits.put("total_amount", String.valueOf(totalAmount));

        // კალათის (Basket) ბლოკი
        Map<String, Object> basketItem = new HashMap<>();
        basketItem.put("quantity", 1);
        basketItem.put("unit_price", String.valueOf(totalAmount));
        basketItem.put("product_id", "cart_items");

        purchaseUnits.put("basket", Collections.singletonList(basketItem));

        requestBody.put("purchase_units", purchaseUnits);

        // Redirect ლინკების ბლოკი
        Map<String, String> redirectUrls = new HashMap<>();
        redirectUrls.put("fail", failUrl);
        redirectUrls.put("success", successUrl);

        requestBody.put("redirect_urls", redirectUrls);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(orderUrl, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode links = root.path("_links");
            if (links.has("redirect")) {
                return links.path("redirect").path("href").asText();
            }
        } catch (Exception e) {
            System.err.println("❌ გადახდის ლინკის გენერაცია ვერ მოხერხდა: " + e.getMessage());
        }

        return null;
    }
}

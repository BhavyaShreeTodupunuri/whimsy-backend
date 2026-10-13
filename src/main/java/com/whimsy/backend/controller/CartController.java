
package com.whimsy.backend.controller;

import com.whimsy.backend.entity.Product;
import com.whimsy.backend.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final ProductRepository productRepository;

    public CartController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Object saved = session.getAttribute("whimsyCart");

        if (saved instanceof Map<?, ?>) {
            return (Map<Long, Integer>) saved;
        }

        Map<Long, Integer> cart = new LinkedHashMap<>();
        session.setAttribute("whimsyCart", cart);
        return cart;
    }

    @GetMapping
    public ResponseEntity<?> getCartItems(HttpSession session) {
        return buildCart(getCart(session));
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(
            @RequestBody Map<String, Object> request,
            HttpSession session) {

        try {
            Long productId = Long.valueOf(
                request.get("productId").toString()
            );
            int quantity = Integer.parseInt(
                request.getOrDefault("quantity", 1).toString()
            );

            if (quantity < 1) {
                return error(HttpStatus.BAD_REQUEST,
                    "Quantity must be at least 1.");
            }

            Product product = productRepository.findById(productId)
                .orElse(null);

            if (product == null) {
                return error(HttpStatus.NOT_FOUND,
                    "Product not found.");
            }

            Map<Long, Integer> cart = getCart(session);
            int newQuantity = cart.getOrDefault(productId, 0) + quantity;

            if (newQuantity > product.getStock()) {
                return error(HttpStatus.BAD_REQUEST,
                    "Not enough stock available.");
            }

            cart.put(productId, newQuantity);
            session.setAttribute("whimsyCart", cart);

            return buildCart(cart);

        } catch (Exception ex) {
            return error(HttpStatus.BAD_REQUEST,
                "Invalid product or quantity.");
        }
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<?> updateItem(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            HttpSession session) {

        try {
            int quantity = Integer.parseInt(
                request.get("quantity").toString()
            );

            Map<Long, Integer> cart = getCart(session);

            if (!cart.containsKey(id)) {
                return error(HttpStatus.NOT_FOUND,
                    "Cart item not found.");
            }

            if (quantity <= 0) {
                cart.remove(id);
            } else {
                Product product = productRepository.findById(id)
                    .orElse(null);

                if (product == null) {
                    cart.remove(id);
                } else if (quantity > product.getStock()) {
                    return error(HttpStatus.BAD_REQUEST,
                        "Not enough stock available.");
                } else {
                    cart.put(id, quantity);
                }
            }

            session.setAttribute("whimsyCart", cart);
            return buildCart(cart);

        } catch (Exception ex) {
            return error(HttpStatus.BAD_REQUEST,
                "Invalid quantity.");
        }
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> removeItem(
            @PathVariable Long id,
            HttpSession session) {

        Map<Long, Integer> cart = getCart(session);
        cart.remove(id);
        session.setAttribute("whimsyCart", cart);

        return buildCart(cart);
    }

    private ResponseEntity<?> buildCart(Map<Long, Integer> cart) {
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Product product = productRepository
                .findById(entry.getKey()).orElse(null);

            if (product == null) {
                continue;
            }

            int quantity = entry.getValue();
            BigDecimal lineTotal = product.getPrice()
                .multiply(BigDecimal.valueOf(quantity));

            Map<String, Object> productData = new LinkedHashMap<>();
            productData.put("id", product.getId());
            productData.put("name", product.getName());
            productData.put("price", product.getPrice());
            productData.put("imageUrl", product.getImageUrl());
            productData.put("stock", product.getStock());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", product.getId());
            item.put("quantity", quantity);
            item.put("product", productData);
            item.put("lineTotal", lineTotal);

            items.add(item);
            subtotal = subtotal.add(lineTotal);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("subtotal", subtotal);
        result.put("total", subtotal.add(BigDecimal.valueOf(30)));

        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status, String message) {

        return ResponseEntity.status(status)
            .body(Map.of("message", message));
    }
}


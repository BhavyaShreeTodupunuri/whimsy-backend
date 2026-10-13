
package com.whimsy.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whimsy.backend.entity.OrderRecord;
import com.whimsy.backend.entity.Product;
import com.whimsy.backend.repository.OrderRepository;
import com.whimsy.backend.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderController(
            OrderRepository orderRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Object saved = session.getAttribute("whimsyCart");
        if (saved instanceof Map<?, ?>) {
            return (Map<Long, Integer>) saved;
        }
        return new LinkedHashMap<>();
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        try {
            Object sessionUserId = session.getAttribute("userId");
            if (!(sessionUserId instanceof Long)) {
                return error(HttpStatus.UNAUTHORIZED,
                        "Please sign in before placing an order.");
            }

            Map<String, Integer> cart = new LinkedHashMap<>();
            for (Map.Entry<Long, Integer> entry
                    : getCart(session).entrySet()) {
                cart.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            if (cart.isEmpty()) {
                return error(HttpStatus.BAD_REQUEST,
                        "Your bag is empty.");
            }

            Map<?, ?> customer = (Map<?, ?>) request.get("customer");
            Map<?, ?> address = (Map<?, ?>) request.get("address");

            if (customer == null || address == null) {
                return error(HttpStatus.BAD_REQUEST,
                        "Customer and delivery details are required.");
            }

            String fullName = value(customer, "fullName");
            String email = value(customer, "email");
            String phone = value(customer, "phone");
            String street = value(address, "street");
            String city = value(address, "city");
            String state = value(address, "state");
            String pincode = value(address, "pincode");

            if (fullName.isBlank() || email.isBlank()
                    || phone.isBlank() || street.isBlank()
                    || city.isBlank() || state.isBlank()
                    || !pincode.matches("[0-9]{6}")) {
                return error(HttpStatus.BAD_REQUEST,
                        "Please provide all required delivery details.");
            }

            List<Map<String, Object>> items = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;

            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                Long productId = Long.valueOf(entry.getKey());
                int quantity = entry.getValue();

                Product product = productRepository.findById(productId)
                        .orElse(null);

                if (product == null || quantity < 1
                        || quantity > product.getStock()) {
                    return error(HttpStatus.BAD_REQUEST,
                            "A cart item is unavailable or has insufficient stock.");
                }

                BigDecimal lineTotal = product.getPrice()
                        .multiply(BigDecimal.valueOf(quantity));
                subtotal = subtotal.add(lineTotal);

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
            }

            OrderRecord order = new OrderRecord();
            order.setUserId((Long) sessionUserId);
            order.setFullName(fullName);
            order.setEmail(email.trim().toLowerCase());
            order.setPhone(phone);
            order.setStreet(street);
            order.setCity(city);
            order.setState(state);
            order.setPincode(pincode);
            order.setSubtotal(subtotal);
            order.setTotal(subtotal.add(BigDecimal.valueOf(30)));
            order.setStatus("PLACED");
            order.setCreatedAt(LocalDateTime.now());
            order.setItemsJson(mapper.writeValueAsString(items));

            OrderRecord saved = orderRepository.save(order);

            session.setAttribute("whimsyCart", new LinkedHashMap<Long, Integer>());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toResponse(saved));

        } catch (Exception ex) {
            return error(HttpStatus.BAD_REQUEST,
                    "Unable to place order. Please check your details and try again.");
        }
    }

    @GetMapping
    public ResponseEntity<?> getOrders(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (!(userId instanceof Long)) {
            return error(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }

        List<Map<String, Object>> orders = orderRepository
                .findAllByUserIdOrderByCreatedAtDesc((Long) userId)
                .stream().map(this::toResponse).toList();

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(
            @PathVariable Long id, HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (!(userId instanceof Long)) {
            return error(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }

        OrderRecord order = orderRepository.findById(id).orElse(null);
        if (order == null || !userId.equals(order.getUserId())) {
            return error(HttpStatus.NOT_FOUND, "Order not found.");
        }

        return ResponseEntity.ok(toResponse(order));
    }

    private Map<String, Object> toResponse(OrderRecord order) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", order.getId());
        result.put("createdAt", order.getCreatedAt());
        result.put("status", order.getStatus());
        result.put("subtotal", order.getSubtotal());
        result.put("total", order.getTotal());
        result.put("paymentMethod", "COD");
        result.put("customer", Map.of(
                "fullName", order.getFullName(),
                "email", order.getEmail(),
                "phone", order.getPhone()));
        result.put("address", Map.of(
                "street", order.getStreet(),
                "city", order.getCity(),
                "state", order.getState(),
                "pincode", order.getPincode()));

        try {
            List<Map<String, Object>> items = mapper.readValue(
                    order.getItemsJson(),
                    new TypeReference<List<Map<String, Object>>>() {});
            result.put("items", items);
        } catch (Exception ex) {
            result.put("items", List.of());
        }

        return result;
    }

    private String value(Map<?, ?> data, String key) {
        Object v = data.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("message", message));
    }
}

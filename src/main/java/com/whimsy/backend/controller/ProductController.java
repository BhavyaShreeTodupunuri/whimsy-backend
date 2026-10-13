
package com.whimsy.backend.controller;

import com.whimsy.backend.entity.Product;
import com.whimsy.backend.repository.ProductRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/test")
    public String testProducts() {
        return "Whimsy Products API is working!";
    }

    @GetMapping
    public List<Product> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {

        List<Product> products = productRepository.findAll();

        // Filter products by category
        if (category != null && !category.isBlank()) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null
                            && p.getCategory().toString()
                                .equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        // Search products by name
        if (search != null && !search.isBlank()) {
            String keyword = search.toLowerCase();

            products = products.stream()
                    .filter(p -> p.getName() != null
                            && p.getName().toLowerCase()
                                .contains(keyword))
                    .collect(Collectors.toList());
        }

        // Sort products by price
        if ("price_asc".equalsIgnoreCase(sort)) {
            products.sort((a, b) ->
                    a.getPrice().compareTo(b.getPrice()));
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            products.sort((a, b) ->
                    b.getPrice().compareTo(a.getPrice()));
        }

        return products;
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Product addProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }
}

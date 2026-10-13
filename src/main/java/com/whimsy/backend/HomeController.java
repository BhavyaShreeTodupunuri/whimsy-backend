
package com.whimsy.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Welcome to Whimsy Backend! 🌸";
    }

    @GetMapping("/api/health")
    public String health() {
        return "Whimsy API is running successfully!";
    }
}

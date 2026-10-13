
package com.whimsy.backend.controller;

import com.whimsy.backend.entity.User;
import com.whimsy.backend.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Register a new customer
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        if (user.getName() == null || user.getName().isBlank()
                || user.getEmail() == null || user.getEmail().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Name, email and password are required."
            ));
        }

        String email = user.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "An account with this email already exists."
            ));
        }

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", savedUser.getId(),
                "name", savedUser.getName(),
                "email", savedUser.getEmail(),
                "phone", savedUser.getPhone(),
                "message", "Account created successfully."
        ));
    }

    // Log in an existing customer and create a server-side session
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody User loginRequest,
            HttpSession session) {

        if (loginRequest.getEmail() == null
                || loginRequest.getEmail().isBlank()
                || loginRequest.getPassword() == null
                || loginRequest.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Email and password are required."
            ));
        }

        String email = loginRequest.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null
                || !passwordEncoder.matches(
                        loginRequest.getPassword(),
                        user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "Invalid email or password."
            ));
        }

        session.setAttribute("userId", user.getId());

        return ResponseEntity.ok(Map.of(
                "user", publicUser(user)
        ));
    }

    // Return the currently logged-in customer
    @GetMapping("/me")
    public ResponseEntity<?> currentUser(HttpSession session) {

        Object userId = session.getAttribute("userId");

        if (!(userId instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "Please sign in."
            ));
        }

        Optional<User> result = userRepository.findById((Long) userId);

        if (result.isEmpty()) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "Please sign in again."
            ));
        }

        return ResponseEntity.ok(publicUser(result.get()));
    }

    // Update the logged-in customer's name and phone
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @RequestBody User updates,
            HttpSession session) {

        Object userId = session.getAttribute("userId");

        if (!(userId instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "Please sign in."
            ));
        }

        Optional<User> result = userRepository.findById((Long) userId);

        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "Please sign in again."
            ));
        }

        User user = result.get();

        if (updates.getName() != null && !updates.getName().isBlank()) {
            user.setName(updates.getName().trim());
        }

        if (updates.getPhone() != null) {
            user.setPhone(updates.getPhone().trim());
        }

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "user", publicUser(savedUser),
                "message", "Profile updated successfully."
        ));
    }

    // Log out the current customer
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully."
        ));
    }

    // Never return the password to the frontend
    private Map<String, Object> publicUser(User user) {
        return Map.of(
                "id", user.getId(),
                "name", user.getName() == null ? "" : user.getName(),
                "email", user.getEmail() == null ? "" : user.getEmail(),
                "phone", user.getPhone() == null ? "" : user.getPhone()
        );
    }
}

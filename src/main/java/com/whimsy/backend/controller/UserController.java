
package com.whimsy.backend.controller;

import com.whimsy.backend.entity.User;
import com.whimsy.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, Object> data) {

        // The current frontend sends profile fields without an email.
        // Use the email only if it is present; otherwise return a clear error.
        Object emailValue = data.get("email");

        if (emailValue == null || emailValue.toString().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message",
                "The frontend must send the signed-in user's email to update the profile."
            ));
        }

        String email = emailValue.toString().trim().toLowerCase();

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                "message", "User not found."
            ));
        }

        User user = optionalUser.get();

        if (data.get("name") instanceof String name) {
            user.setName(name.trim());
        }

        if (data.get("phone") instanceof String phone) {
            user.setPhone(phone.trim());
        }

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "id", savedUser.getId(),
            "name", savedUser.getName() == null ? "" : savedUser.getName(),
            "email", savedUser.getEmail(),
            "phone", savedUser.getPhone() == null ? "" : savedUser.getPhone(),
            "message", "Profile updated successfully."
        ));
    }
}

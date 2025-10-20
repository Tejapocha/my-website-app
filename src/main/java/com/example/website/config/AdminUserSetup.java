package com.example.website.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.website.model.Role;
import com.example.website.model.User; // 💡 Assuming this is the correct path for User model
import com.example.website.repository.UserRepository;

import java.util.Optional;

@Component
public class AdminUserSetup implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminUserSetup(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Executes logic immediately after the Spring application context has loaded.
     */
    @Override
    public void run(String... args) {
        final String username = "Teja";
        final String rawPassword = "Teja1!2";
        
        Optional<User> existingUser = userRepo.findByUsername(username);

        if (existingUser.isPresent()) {
            // ✅ Case 1: User already exists, ensure they have the ADMIN role and correct password
            User user = existingUser.get();
            
            // Check if the user is already ADMIN to avoid unnecessary DB write
            if (user.getRole() != Role.ADMIN || !passwordEncoder.matches(rawPassword, user.getPassword())) {
                user.setRole(Role.ADMIN);
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepo.save(user);
                System.out.println("Admin user '" + username + "' updated successfully. 🛠️");
            } else {
                System.out.println("Admin user '" + username + "' already configured. Skipping. 🚀");
            }
            
        } else {
            // 🛑 Case 2: User does not exist, so CREATE them
            User newAdmin = new User();
            newAdmin.setUsername(username);
            newAdmin.setPassword(passwordEncoder.encode(rawPassword)); // Store the HASHED password
            newAdmin.setRole(Role.ADMIN);
            
            // 💡 Assuming your User model also requires an email/name, you should set them here:
            // newAdmin.setEmail("teja@admin.com");
            
            userRepo.save(newAdmin);
            System.out.println("Admin user '" + username + "' created successfully! 🎉");
        }
    }
}
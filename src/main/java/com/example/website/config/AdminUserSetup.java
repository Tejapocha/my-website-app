package com.example.website.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.website.model.Role;
import com.example.website.model.User; 
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

    @Override
    public void run(String... args) {
        final String username = "Teja";
        final String rawPassword = "Teja1!2";
        
        // 💡 RECOMMENDATION: For production, use an environment variable or config file 
        // to set a default admin password, not hardcoded.

        Optional<User> existingUser = userRepo.findByUsername(username);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            boolean needsUpdate = false;
            
            // Check for correct role
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                needsUpdate = true;
            }
            
            // Check for correct password (only update if it's incorrect)
            if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                needsUpdate = true;
            }

            if (needsUpdate) {
                userRepo.save(user);
                System.out.println("Admin user '" + username + "' updated successfully (Role and/or Password). 🛠️");
            } else {
                System.out.println("Admin user '" + username + "' already configured. Skipping. 🚀");
            }
            
        } else {
            // Case: User does not exist, so CREATE them
            User newAdmin = new User();
            newAdmin.setUsername(username);
            newAdmin.setPassword(passwordEncoder.encode(rawPassword)); 
            newAdmin.setRole(Role.ADMIN);
            newAdmin.setEmail("teja@admin.com"); // Added default email
            newAdmin.setName("Teja Admin"); // Added default name
            
            userRepo.save(newAdmin);
            System.out.println("Admin user '" + username + "' created successfully! 🎉");
        }
    }
}
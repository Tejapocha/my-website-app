package com.example.website.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.website.model.Role;
import com.example.website.repository.UserRepository;

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
        userRepo.findByUsername("Teja").ifPresent(user -> {
            user.setRole(Role.ADMIN);
            user.setPassword(passwordEncoder.encode("Teja1!2"));
            userRepo.save(user);
            System.out.println("Admin user updated!");
        });
    }
}

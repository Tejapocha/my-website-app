package com.example.website.service;

import com.example.website.model.User;
import com.example.website.model.Role;
import com.example.website.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with a hashed password and the default USER role.
     * @param user The User object containing raw username, email, and password.
     * @return The saved User entity.
     * @throws IllegalStateException if the username or email already exists.
     */
    @Transactional
    public User registerNewUser(User user) {
        // 1. Validation: Check for existing username or email
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalStateException("Username is already taken.");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("Email is already in use.");
        }
        
        // 2. Encode Password and Set Default Role
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        
        user.setPassword(hashedPassword);
        user.setRole(Role.USER); // Default role for new registrations

        // 3. Save to database
        return userRepository.save(user);
    }
    
    // Optional: Check if a user exists by ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
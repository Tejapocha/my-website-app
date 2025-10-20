package com.example.website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.website.model.User;
import java.util.Optional;
import org.springframework.stereotype.Repository; // Recommended for clarity

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username. Used for Spring Security login.
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Finds a user by their email address. Used to prevent duplicate registrations.
     */
    Optional<User> findByEmail(String email); 
}
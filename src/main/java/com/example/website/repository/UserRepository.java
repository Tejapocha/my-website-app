package com.example.website.repository;

import com.example.website.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity persistence operations.
 * Spring Data JPA automatically provides CRUD methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a User entity by their unique username.
     * Used for login (by Spring Security) and checking for duplicates during registration.
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a User entity by their unique email address.
     * Used for checking for duplicates during registration.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user with the given username already exists.
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user with the given email already exists.
     */
    boolean existsByEmail(String email);
}

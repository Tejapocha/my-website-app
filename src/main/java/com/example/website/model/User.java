package com.example.website.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Must be unique for login (primary identifier)
    @Column(unique = true, nullable = false)
    private String username;

    // Must store the HASHED password
    @Column(nullable = false, length = 60) // 💡 Hashed passwords (BCrypt) require a length of 60
    private String password;

    // 🟢 ADDED: Email field for account recovery and profile
    @Column(unique = true, nullable = true)
    private String email; 
    
    // 🟢 ADDED: Name field for profile display
    private String name; 

    // 💡 FIX 1: Default role should be USER, not ADMIN.
    // The ADMIN role should only be assigned explicitly via initial setup (AdminUserSetup) or migration.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER; 

    // Default Constructor (Required by JPA)
    public User() {}

    // Parameterized Constructor for registration (excluding ID)
    public User(String username, String password, String email, String name, Role role) {
        this.username = username;
        this.password = password; // Should be the HASHED password
        this.email = email;
        this.name = name;
        this.role = role;
    }


    // Getters and setters
    
    public Long getId() { return id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    // ⚠️ CRITICAL SECURITY NOTE: This setter should ONLY be called with an encoded password!
    public void setPassword(String password) { this.password = password; } 
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
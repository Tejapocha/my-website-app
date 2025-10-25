package com.example.website.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(unique = true, nullable = true)
    private String email; 
    
    private String name; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER; 

    // Default Constructor (Ensures non-null defaults for new entities)
    public User() {
        this.username = "";
        this.password = "";
        this.email = ""; // Default empty string or null if allowed
        this.name = "";  // Default empty string or null if allowed
        this.role = Role.USER;
    }
    // ... (rest of the code is unchanged)
    
    // Getters and setters (omitted for brevity, assume they are correct)
    
    public Long getId() { return id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; } 
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
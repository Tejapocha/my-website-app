package com.example.website.model;

import jakarta.persistence.*;

/**
 * Represents a single like action taken by a user on a piece of content.
 * The UniqueConstraint ensures a user can only have one like record per content.
 */
@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "content_id"})
})
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Assuming user authentication is in place, this stores the ID of the user who liked the content.
    @Column(name = "user_id", nullable = false)
    private Long userId; 

    // Stores the ID of the content (video/image) that was liked.
    @Column(name = "content_id", nullable = false)
    private Long contentId; 

    // Default Constructor
    public Like() {}

    // Parameterized Constructor
    public Like(Long userId, Long contentId) {
        this.userId = userId;
        this.contentId = contentId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    
    @Override
    public String toString() {
        return "Like{" +
                "id=" + id +
                ", userId=" + userId +
                ", contentId=" + contentId +
                '}';
    }
}
package com.example.website.model;

import jakarta.persistence.*;
import java.io.Serializable;

// 💡 NOTE: This entity uses a Composite Primary Key (PK) defined by the LikeId class.
// This enforces that a user can only 'like' a piece of content once.

@Entity
@Table(name = "likes")
@IdClass(Like.LikeId.class) // Declares the class for the Composite Primary Key
public class Like implements Serializable {

    // --- Composite Primary Key Fields ---
    @Id
    @Column(name = "content_id")
    private Long contentId;

    @Id
    @Column(name = "user_id")
    private Long userId;
    // ------------------------------------

    // Default constructor for JPA
    public Like() {}

    // Constructor for creating a new like
    public Like(Long contentId, Long userId) {
        this.contentId = contentId;
        this.userId = userId;
    }
    
    // --- Inner Class for Composite Primary Key ---
    // Required by JPA when using multiple fields as a primary key
    public static class LikeId implements Serializable {
        private Long contentId;
        private Long userId;

        // Constructors, getters, setters, hashCode, and equals must be implemented
        public LikeId() {}
        
        // standard getters and setters
        public Long getContentId() { return contentId; }
        public void setContentId(Long contentId) { this.contentId = contentId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        @Override
        public int hashCode() {
            return (int) (contentId + userId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            LikeId other = (LikeId) obj;
            return contentId.equals(other.contentId) && userId.equals(other.userId);
        }
    }
    
    // --- Getters and Setters for Like Entity ---

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}

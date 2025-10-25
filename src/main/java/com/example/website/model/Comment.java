package com.example.website.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 💡 CRITICAL FIX: Define the Many-to-One relationship to Content.
    // This establishes the foreign key relationship managed by JPA.
    // The @JoinColumn specifies the actual column name in the database table ("content_id").
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content; // The object reference back to the Content

    // The username who posted the comment (since we fetch the username in the controller)
    @Column(name = "user_name", nullable = false)
    private String userName;

    // The actual comment text
    @Column(name = "comment_text", nullable = false, length = 500)
    private String text;

    @Column(name = "post_date", nullable = false)
    private LocalDateTime postDate;

    // --- Constructors ---

    public Comment() {
        this.postDate = LocalDateTime.now();
    }
    
    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // 💡 CRITICAL: Getters/Setters now use the Content object
    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    // You can keep a helper getter if needed, but the object is preferred for JPA operations
    public Long getContentId() {
        if (content != null) {
            return content.getId();
        }
        return null;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getPostDate() {
        return postDate;
    }

    public void setPostDate(LocalDateTime postDate) {
        this.postDate = postDate;
    }
}
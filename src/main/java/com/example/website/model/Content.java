package com.example.website.model;

import jakarta.persistence.*;

@Entity
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(length = 255, nullable = false)
    private String filePath;

    @Column(length = 50)
    private String fileType;

    // Likes column with default value 0 to avoid NOT NULL errors
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int likes = 0;

    // Comments column (nullable, can be empty initially)
    @Column(length = 1000)
    private String comments = "";

    // Default constructor
    public Content() {}

    // Constructor
    public Content(String title, String description, String filePath, String fileType) {
        this.title = title;
        this.description = description;
        this.filePath = filePath;
        this.fileType = fileType;
        this.likes = 0;
        this.comments = "";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    @Override
    public String toString() {
        return "Content{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileType='" + fileType + '\'' +
                ", likes=" + likes +
                ", comments='" + comments + '\'' +
                '}';
    }
}

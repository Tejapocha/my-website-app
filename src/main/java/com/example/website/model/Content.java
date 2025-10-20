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

    // ✅ Likes column with default 0
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int likes = 0;

    // ✅ View count
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int views = 0;

    // ✅ Comments (optional)
    @Column(length = 2000)
    private String comments = "";

    // ✅ Tags (e.g., "sports,funny,celebrity")
    @Column(length = 255)
    private String tags = "";

    // ✅ Default constructor
    public Content() {}

    // ✅ Constructor with basic fields
    public Content(String title, String description, String filePath, String fileType) {
        this.title = title;
        this.description = description;
        this.filePath = filePath;
        this.fileType = fileType;
        this.likes = 0;
        this.views = 0;
        this.comments = "";
        this.tags = "";
    }

    // ✅ Getters and Setters
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

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    @Override
    public String toString() {
        return "Content{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileType='" + fileType + '\'' +
                ", likes=" + likes +
                ", views=" + views +
                ", tags='" + tags + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}

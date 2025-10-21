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

    // Likes count, default 0
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer likes = 0;

    // Views count, default 0
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer views = 0;

    // Comments (optional)
    @Column(length = 2000)
    private String comments = "";

    // Tags (optional)
    @Column(length = 255)
    private String tags = "";

    // Default constructor
    public Content() {}

    // Constructor with basic fields
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

    // Getters and setters (Integer types to avoid null issues)
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

    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes != null ? likes : 0; }

    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views != null ? views : 0; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments != null ? comments : ""; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags != null ? tags : ""; }

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
                ", comments='" + comments + '\'' +
                ", tags='" + tags + '\'' +
                '}';
    }
}

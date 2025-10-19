package com.example.website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.website.model.Content;
import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {
    
    // ✅ Search content by title or description (case-insensitive)
    List<Content> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
}

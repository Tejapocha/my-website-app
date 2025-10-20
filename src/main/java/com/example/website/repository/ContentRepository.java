package com.example.website.repository;

import com.example.website.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContentRepository extends JpaRepository<Content, Long> {

    // Search (for both title and description)
    List<Content> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    // ✅ Paginated search
    Page<Content> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);

    // ✅ Count results for pagination
    long countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    List<Content> findTop10ByOrderByLikesDesc();
    List<Content> findTop10ByOrderByViewsDesc();
}

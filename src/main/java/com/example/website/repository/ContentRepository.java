package com.example.website.repository;

import com.example.website.model.Content;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long>, JpaSpecificationExecutor<Content> {

    // --- Search & Filtering Methods (Used by ContentService) ---

    /**
     * 💡 NEW METHOD: Finds content where the title OR tags contain the specified keyword (case-insensitive and paginated).
     */
    Page<Content> findByTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(String title, String tagKeyword, Pageable pageable);
    
    /**
     * Finds content by matching the title (used for the general search/keyword filter).
     * The service combines this with the findAll(Pageable) for the final result.
     */
    Page<Content> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    /**
     * Finds content where the 'tags' string contains the specified keyword (used for the tag filter).
     */
    Page<Content> findByTagsContainingIgnoreCase(String tagKeyword, Pageable pageable);

    // --- Interaction Methods ---

    /**
     * REQUIRED FOR incrementViews(): Updates the view count for a given content ID.
     */
    @Transactional
    @Modifying
    @Query("UPDATE Content c SET c.views = c.views + 1 WHERE c.id = :contentId")
    void incrementViews(Long contentId);

   // --- Legacy / Unpaginated Methods (Optional but kept for completeness) ---
    
    // Search (for both title and description) - Unpaginated
    List<Content> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    // Paginated search (kept for flexibility)
    Page<Content> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);

    // Count results for pagination (kept for flexibility)
    long countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    // Count by Tag (kept for flexibility)
    long countByTagsContainingIgnoreCase(String tagKeyword);
    
    // Top 10 Most Liked
    List<Content> findTop10ByOrderByLikesDesc();

    // Top 10 Most Viewed
    List<Content> findTop10ByOrderByViewsDesc();
}
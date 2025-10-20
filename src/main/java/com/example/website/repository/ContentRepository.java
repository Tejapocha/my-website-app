package com.example.website.repository;

import com.example.website.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository // Optional but good practice for clarity
public interface ContentRepository extends JpaRepository<Content, Long> {

    // Search (for both title and description) - Unpaginated
    List<Content> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    // ✅ Paginated search
    Page<Content> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);

    // ✅ Count results for pagination
    long countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    // ✅ Top 10 Most Liked
    List<Content> findTop10ByOrderByLikesDesc();
    
    // ✅ Top 10 Most Viewed
    List<Content> findTop10ByOrderByViewsDesc();

    // ----------------------------------------------------------------------
    // 💡 NEW METHOD for Tag-Based Filtering (e.g., "Celebrity Videos")
    // ----------------------------------------------------------------------

    /**
     * Finds content where the 'tags' string contains the specified keyword (tag).
     * Useful for filtering by category (e.g., tag="celebrity").
     * * @param tagKeyword The specific tag to search for (e.g., "celebrity").
     * @param pageable Pagination and sorting information.
     * @return A Page of Content matching the tag.
     */
    Page<Content> findByTagsContainingIgnoreCase(String tagKeyword, Pageable pageable);
    
    /**
     * Counts the total number of records matching a specific tag for pagination.
     * * @param tagKeyword The specific tag to search for.
     * @return The count of matching Content records.
     */
    long countByTagsContainingIgnoreCase(String tagKeyword);
}
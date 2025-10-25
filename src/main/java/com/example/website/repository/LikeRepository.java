package com.example.website.repository;

import com.example.website.model.Like;
import com.example.website.model.Like.LikeId; // Import the inner class
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, LikeId> {

    /**
     * Finds a specific Like record by the combination of contentId and userId.
     * Required for: ContentService.toggleLike() and ContentService.isLikedByUser()
     */
    Optional<Like> findByContentIdAndUserId(Long contentId, Long userId);
    
    /**
     * 
     * Retrieves a list of Content IDs that a specific user has liked.
     * Required for: ContentService.getLikedContentIds()
     */
    @Query("SELECT l.contentId FROM Like l WHERE l.userId = :userId")
    List<Long> findContentIdsByUserId(Long userId);
}

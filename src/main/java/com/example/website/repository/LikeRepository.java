package com.example.website.repository;

import com.example.website.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    /**
     * CRITICAL for "One like per user": Finds a Like record based on the
     * specific combination of User ID and Content ID.
     * * @param userId The ID of the authenticated user.
     * @param contentId The ID of the content being checked.
     * @return An Optional containing the Like entity if found, or empty otherwise.
     */
    Optional<Like> findByUserIdAndContentId(Long userId, Long contentId);
}
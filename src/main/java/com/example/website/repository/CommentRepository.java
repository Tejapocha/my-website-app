package com.example.website.repository;

import com.example.website.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Finds all comments associated with a specific content ID,
     * typically ordered by post date (newest or oldest first).
     * This is useful for displaying comments on the content-detail page.
     */
   
 // In com.example.website.repository.CommentRepository.java

 // The attribute name in the Comment entity is 'content', 
 // and we're querying by the 'id' of that 'content' object.
 List<Comment> findByContent_IdOrderByPostDateDesc(Long contentId);
    // The JpaRepository interface already provides:
    // - save(Comment comment): Used by contentService.addComment()
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
}
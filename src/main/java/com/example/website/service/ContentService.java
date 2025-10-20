package com.example.website.service;

import com.example.website.model.Content;
import com.example.website.model.Like; // 🟢 Import the Like Model
import com.example.website.repository.ContentRepository;
import com.example.website.repository.LikeRepository; // 🟢 Import the Like Repository
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ContentService {

    @Autowired
    private ContentRepository repo;

    @Autowired // 🟢 Autowire the LikeRepository
    private LikeRepository likeRepo; 


    // ✅ Save or update content
    public Content save(Content content) {
        return repo.save(content);
    }

    // ----------------------------------------------------------------------
    // PAGINATION AND SEARCH METHODS
    // ----------------------------------------------------------------------

    // ✅ Get all uploaded content (no pagination)
    public List<Content> getAll() {
        return repo.findAll();
    }
    
    // ✅ Pagination support (10 per page, base method)
    // NOTE: This is technically redundant if getPaginated is used, but kept for compatibility.
    public Page<Content> getPaginatedContents(int page) {
        // Correcting to 0-based index for PageRequest, and pageSize=10
        return repo.findAll(PageRequest.of(page - 1, 10, Sort.by(Sort.Direction.DESC, "id"))); 
    }
    
    // ✅ Get paginated + searchable results
    public Page<Content> getPaginated(String keyword, int page, int pageSize) {
        // PageRequest is 0-indexed, so we subtract 1 from the user-visible page number
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        if (keyword == null || keyword.trim().isEmpty()) {
            return repo.findAll(pageable);
        } else {
            return repo.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable);
        }
    }

    // ✅ Get total pages for pagination (Handles 1-based index calculation)
    public int getTotalPages(String keyword, int pageSize) {
        long totalRecords;
        if (keyword == null || keyword.trim().isEmpty()) {
            totalRecords = repo.count();
        } else {
            totalRecords = repo.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
        }
        // Calculation: Math.ceil(total / pageSize)
        return (int) Math.ceil((double) totalRecords / pageSize);
    }

    // ----------------------------------------------------------------------
    // SORTING/UTILITY METHODS
    // ----------------------------------------------------------------------

    // ✅ Get most liked content (Top 10)
    public List<Content> getMostLiked() {
        return repo.findTop10ByOrderByLikesDesc();
    }

    // ✅ Get most viewed content (Top 10)
    public List<Content> getMostViewed() {
        return repo.findTop10ByOrderByViewsDesc();
    }

    // ✅ Search by title or description
    public List<Content> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return repo.findAll();
        }
        return repo.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
    }
    
    // ✅ Get content by ID safely
    public Content getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content not found with ID: " + id));
    }
    
    // ✅ Delete content by ID
    public void delete(Long id) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
        } else {
            // Using the standard JPA exception type
            throw new EntityNotFoundException("Content with ID " + id + " not found for deletion");
        }
    }

    // ----------------------------------------------------------------------
    // CORE INTERACTION METHODS
    // ----------------------------------------------------------------------
    
    // 🟢 SECURELY Toggles the like status (LIKE or UNLIKE)
    @Transactional
    public void toggleLike(Long contentId, Long userId) {
        
        // 1. Fetch the Content
        Content content = repo.findById(contentId)
                .orElseThrow(() -> new EntityNotFoundException("Content not found with ID: " + contentId));

        // 2. Check for existing Like record for this user and content
        Optional<Like> existingLike = likeRepo.findByUserIdAndContentId(userId, contentId);

        if (existingLike.isPresent()) {
            // 3a. UNLIKE: Delete the record and decrement the count
            likeRepo.delete(existingLike.get());
            if (content.getLikes() > 0) { // Safety check
                content.setLikes(content.getLikes() - 1);
            }
        } else {
            // 3b. LIKE: Create the record and increment the count
            Like newLike = new Like(userId, contentId); 
            likeRepo.save(newLike);
            
            content.setLikes(content.getLikes() + 1);
        }
        
        // 4. Persist the updated Content (likes count)
        repo.save(content); 
    }
    
    // ✅ Increment view count
    @Transactional 
    public void incrementViews(Long id) {
        Content content = getById(id); // Uses the safe getById method
        content.setViews(content.getViews() + 1);
        repo.save(content);
    }
}
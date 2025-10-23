package com.example.website.service;

import com.example.website.model.Content;
import com.example.website.model.Like;
import com.example.website.repository.ContentRepository;
import com.example.website.repository.LikeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
public class ContentService {

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    // Helper to determine file type from MIME type
    private String getFileType(String mimeType) {
        if (mimeType == null) return "unknown";
        if (mimeType.startsWith("image")) return "image";
        if (mimeType.startsWith("video")) return "video";
        return "other";
    }

    // --- UPLOAD AND CRUD METHODS ---

    /**
     * Saves new content (Image or Video) to the database and uploads the file to Cloudinary.
     *
     * @param content The Content entity containing metadata.
     * @param file    The MultipartFile to be uploaded.
     * @return The saved Content entity.
     * @throws IOException If file processing fails.
     */
    @Transactional
    public Content saveContent(Content content, MultipartFile file) throws IOException {
        String filePath = cloudinaryService.uploadFile(file);
        content.setFilePath(filePath);
        content.setFileType(getFileType(file.getContentType()));
        // Initialize other fields
        content.setViews(0);
        content.setLikes(0);
        content.setComments(""); // Initialize comments as empty string

        return contentRepository.save(content);
    }

    /**
     * Deletes content by ID.
     *
     * @param id The ID of the content to delete.
     */
    @Transactional
    public void delete(Long id) {
        if (!contentRepository.existsById(id)) {
            throw new EntityNotFoundException("Content with ID " + id + " not found.");
        }
        contentRepository.deleteById(id);
        // Note: Ideally, you would also call CloudinaryService to delete the file here.
    }

    /**
     * Retrieves content by its ID.
     *
     * @param id The ID of the content.
     * @return The Content entity.
     */
    public Content getById(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content with ID " + id + " not found."));
    }

    // --- INTERACTION METHODS ---

    /**
     * Increments the view count for a piece of content.
     *
     * @param id The ID of the content to update.
     */
    @Transactional
    public void incrementViews(Long id) {
        Optional<Content> optionalContent = contentRepository.findById(id);
        if (optionalContent.isPresent()) {
            Content content = optionalContent.get();
            content.setViews(content.getViews() + 1);
            contentRepository.save(content);
        }
    }

    /**
     * Toggles the like status for a piece of content by a specific user.
     *
     * @param contentId The ID of the content.
     * @param userId    The ID of the user.
     * @return The new total like count for the content.
     */
    @Transactional
    public int toggleLike(Long contentId, Long userId) {
        Content content = getById(contentId);
        Optional<Like> existingLike = likeRepository.findByUserIdAndContentId(userId, contentId);

        if (existingLike.isPresent()) {
            // Unlike
            likeRepository.delete(existingLike.get());
            content.setLikes(content.getLikes() - 1);
        } else {
            // Like
            Like newLike = new Like();
            newLike.setContentId(contentId);
            newLike.setUserId(userId);
            likeRepository.save(newLike);
            content.setLikes(content.getLikes() + 1);
        }

        contentRepository.save(content);
        return content.getLikes();
    }

    /**
     * Adds a new comment to a piece of content.
     * The comment is appended to the existing comments string.
     *
     * @param contentId The ID of the content.
     * @param userName  The name of the user posting the comment.
     * @param commentText The text of the comment.
     */
    @Transactional
    public void addComment(Long contentId, String userName, String commentText) {
        Content content = getById(contentId);

        String existingComments = content.getComments();
        
        // Format the new comment as "[User Name]: Comment Text"
        String newCommentEntry = String.format("\n%s: %s", userName, commentText.trim());

        // Append the new comment. We trim the existing string before appending
        // to avoid leading newlines if the comments field was initialized as ""
        content.setComments((existingComments.trim() + newCommentEntry).trim()); 

        contentRepository.save(content);
    }

    /**
     * Checks if a user has liked a specific piece of content.
     *
     * @param contentId The ID of the content.
     * @param userId    The ID of the user.
     * @return true if the user has liked the content, false otherwise.
     */
    public boolean isLikedByUser(Long contentId, Long userId) {
        return likeRepository.findByUserIdAndContentId(userId, contentId).isPresent();
    }

    // --- PAGINATION AND SEARCH METHODS ---

    /**
     * Retrieves a paginated list of all content, optionally filtered by a keyword.
     *
     * @param keyword  A search string to filter by title, description, or tags, or the ID.
     * @param page     The 1-based page number to retrieve.
     * @param pageSize The number of items per page.
     * @return A Page object containing the results.
     */
    public Page<Content> getPaginated(String keyword, int page, int pageSize) {
        // Spring Data JPA uses 0-based indexing for PageRequest
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, pageSize, Sort.by("id").descending());

        if (keyword == null || keyword.trim().isEmpty()) {
            return contentRepository.findAll(pageable);
        }

        // --- Logic to search by Content ID OR text fields ---
        Long idSearch = null;
        try {
            idSearch = Long.parseLong(keyword.trim());
        } catch (NumberFormatException e) {
            // The keyword is not a number, proceed with text search
        }

        // Create a dynamic Specification (query builder)
        final Long finalIdSearch = idSearch;
        Specification<Content> spec = (root, query, cb) -> {
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";

            // Predicate for text search (title, description, tags)
            jakarta.persistence.criteria.Predicate textPredicate = cb.or(
                    cb.like(cb.lower(root.get("title")), likeKeyword),
                    cb.like(cb.lower(root.get("description")), likeKeyword),
                    cb.like(cb.lower(root.get("tags")), likeKeyword)
            );

            // Predicate for ID search (if keyword is a valid number)
            if (finalIdSearch != null) {
                jakarta.persistence.criteria.Predicate idPredicate = cb.equal(root.get("id"), finalIdSearch);
                // Combine text search OR ID search
                return cb.or(textPredicate, idPredicate);
            }

            // Only perform text search
            return textPredicate;
        };

        return contentRepository.findAll(spec, pageable);
    }

    // --- FILTERING METHODS (for dashboard filters) ---

    /**
     * Retrieves a paginated list of content sorted by the number of likes (most liked first).
     * Used for the "Best Videos" filter.
     */
    public Page<Content> getMostLikedPaginated(int page, int pageSize) {
        // Sort by 'likes' descending
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, pageSize, Sort.by("likes").descending());
        return contentRepository.findAll(pageable);
    }

    /**
     * Retrieves a paginated list of content sorted by the number of views (most viewed first).
     */
    public Page<Content> getMostViewedPaginated(int page, int pageSize) {
        // Sort by 'views' descending
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, pageSize, Sort.by("views").descending());
        return contentRepository.findAll(pageable);
    }
    
    /**
     * **NEW METHOD:** Retrieves a paginated list of content filtered by a specific tag 
     * (used for "Categories" like celebrity, homemade, college).
     * * @param tag The tag string to filter content by.
     * @param page The 1-based page number.
     * @param pageSize The number of items per page.
     * @return A Page object containing the results.
     */
    public Page<Content> getVideosByTagPaginated(String tag, int page, int pageSize) {
        // Sort by ID descending (newest first for the category)
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, pageSize, Sort.by("id").descending());
        // Assumes ContentRepository has a method like: 
        // Page<Content> findByTagsContainingIgnoreCase(String tags, Pageable pageable);
        return contentRepository.findByTagsContainingIgnoreCase(tag, pageable);
    }
}

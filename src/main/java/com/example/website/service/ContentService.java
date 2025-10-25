package com.example.website.service;

import com.example.website.model.Content;
import com.example.website.model.Like; // Assuming you have a Like entity
import com.example.website.model.Comment; // Assuming you have a Comment entity
import com.example.website.repository.ContentRepository;
import com.example.website.repository.LikeRepository; // Assuming this repository exists
import com.example.website.repository.CommentRepository; // Assuming this repository exists
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContentService {

    // Final fields using constructor injection (preferred over field @Autowired)
    private final ContentRepository contentRepository;
    private final CloudinaryService cloudinaryService; // <--- NEW CLOUDINARY DEPENDENCY

    // Autowired fields for repositories that don't need to be final
    @Autowired
    private LikeRepository likeRepository; 
    
    @Autowired
    private CommentRepository commentRepository; 

    // Constructor Injection for ContentRepository and CloudinaryService
    public ContentService(ContentRepository contentRepository, CloudinaryService cloudinaryService) {
        this.contentRepository = contentRepository;
        this.cloudinaryService = cloudinaryService;
    }

    // --- CRUD METHODS ---

    /**
     * Required by: /admin/upload (POST)
     * Uploads the file to Cloudinary and saves content metadata with the resulting public URL.
     */
    public void saveContent(Content content, MultipartFile file) throws IOException {
        
        if (file == null || file.isEmpty()) {
            throw new IOException("Cannot save content: Uploaded file is missing or empty.");
        }
        
        // 💡 CRITICAL FIX: Upload file to Cloudinary and set the public URL as the filePath
        String publicUrl = cloudinaryService.uploadFile(file);
        content.setFilePath(publicUrl);
        
        content.setUploadDate(LocalDateTime.now());
        
        // Determine file type based on MIME type
        String contentType = file.getContentType();
        if (contentType != null) {
            content.setFileType(contentType.startsWith("video") ? "video" : 
                                 contentType.startsWith("image") ? "image" : "other");
        } else {
             content.setFileType("unknown");
        }
        
        // Ensure initial counts are 0 if the model doesn't handle defaults
        if (content.getViews() == null) content.setViews(0);
        if (content.getLikes() == null) content.setLikes(0);
        
        contentRepository.save(content);
    }

    // --- VIEW & INTERACTION METHODS ---

    public List<Comment> getCommentsByContentId(Long contentId) {
        // Ensure the service method call uses the updated repository method name
        return commentRepository.findByContent_IdOrderByPostDateDesc(contentId);
    }
    
    /**
     * Retrieves content by ID.
     */
    public Content getById(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content not found with ID: " + id));
    }

    /**
     * Deletes content by ID.
     */
    public void delete(Long id) {
        if (!contentRepository.existsById(id)) {
            throw new EntityNotFoundException("Content not found with ID: " + id);
        }
        contentRepository.deleteById(id);
    }

    /**
     * Atomically increments the view count for content.
     */
    public void incrementViews(Long contentId) {
        // Ideally done with a custom JPA method or native query for thread safety
        contentRepository.incrementViews(contentId); 
    }

    /**
     * Toggles the like status and updates the Content's like count.
     * @return The new total like count.
     */
    public int toggleLike(Long contentId, Long userId) {
        Content content = getById(contentId);
        Optional<Like> existingLike = likeRepository.findByContentIdAndUserId(contentId, userId);
        
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            content.setLikes(content.getLikes() - 1);
        } else {
            // Note: Your Like model/repository implementation is assumed to handle 
            // the saving of a Like entity with only contentId and userId.
            Like newLike = new Like(contentId, userId);
            likeRepository.save(newLike);
            content.setLikes(content.getLikes() + 1);
        }
        
        contentRepository.save(content);
        return content.getLikes();
    }
    
    /**
     * Gets the list of Content IDs liked by a specific user.
     */
    public List<Long> getLikedContentIds(Long userId) {
        // Assuming LikeRepository has a method to fetch only content IDs
        return likeRepository.findContentIdsByUserId(userId); 
    }
    
    /**
     * Checks if a user has liked a specific piece of content.
     */
    public boolean isLikedByUser(Long contentId, Long userId) {
        return likeRepository.findByContentIdAndUserId(contentId, userId).isPresent();
    }
    
    /**
     * Adds a new comment to the content.
     */
    public void addComment(Long contentId, String userName, String commentText) {
        // 1. Fetch the Content entity
        Content content = contentRepository.findById(contentId)
            .orElseThrow(() -> new EntityNotFoundException("Content not found."));

        // 2. Create the new Comment entity
        Comment comment = new Comment();
        comment.setUserName(userName);
        comment.setText(commentText);
        comment.setPostDate(LocalDateTime.now());

        // 3. Manage the bidirectional relationship using the helper in Content model
        // This sets the content reference on the comment entity: comment.setContent(content);
        content.addComment(comment);

        // 4. Save the parent (Content) which cascades the save to the child (Comment)
        contentRepository.save(content);
    }

    // --- PAGINATION / FILTERING METHODS ---
    
    /**
     * Retrieves content based on keyword or all content, sorted by date.
     */
    public Page<Content> getPaginated(String keyword, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("uploadDate").descending());

        if (keyword != null && !keyword.isEmpty()) {
            return contentRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        } else {
            return contentRepository.findAll(pageable);
        }
    }

    /**
     * Retrieves content sorted by views.
     */
    public Page<Content> getMostViewedPaginated(int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("views").descending());
        return contentRepository.findAll(pageable);
    }

    /**
     * Retrieves content sorted by likes.
     */
    public Page<Content> getMostLikedPaginated(int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("likes").descending());
        return contentRepository.findAll(pageable);
    }

    /**
     * Retrieves content filtered by tag.
     */
    public Page<Content> getVideosByTagPaginated(String tag, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("uploadDate").descending());
        return contentRepository.findByTagsContainingIgnoreCase(tag, pageable);
    }
}

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile; // ❗ NEW IMPORT

import java.io.IOException; // ❗ NEW IMPORT
import java.util.List;
import java.util.Optional;

@Service
public class ContentService {

    @Autowired
    private ContentRepository repo;

    @Autowired 
    private LikeRepository likeRepo; 

    @Autowired
    private CloudinaryService cloudinaryService;

    // ----------------------------------------------------------------------
    // CORE SAVE/UPDATE METHOD (MODIFIED FOR S3 UPLOAD)
    // ----------------------------------------------------------------------
    /**
     * Uploads the file to S3, saves the public URL, and persists the Content entity.
     * @param content The content metadata.
     * @param file The actual file (video or image) to upload.
     * @return The persisted Content object.
     */
    @Transactional
    public Content saveContent(Content content, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                // Upload file to Cloudinary
                String url = cloudinaryService.uploadFile(file);
                content.setFilePath(url);

                // Detect file type
                String contentType = file.getContentType();
                if (contentType != null) {
                    if (contentType.startsWith("image/")) content.setFileType("image");
                    else if (contentType.startsWith("video/")) content.setFileType("video");
                    else throw new RuntimeException("Unsupported file type: " + contentType);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
            }
        }
        // Set default values if needed
        if (content.getLikes() == null) content.setLikes(0);
        if (content.getViews() == null) content.setViews(0);
        if (content.getComments() == null) content.setComments("");
        if (content.getTags() == null) content.setTags("");

        // Save content to database
        return repo.save(content);
    }

    // ... REST OF THE ORIGINAL SERVICE METHODS ...
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
            // NOTE: In a complete S3 solution, you would call s3Service.deleteFile(filePath) here first.
            repo.deleteById(id);
        } else {
            throw new EntityNotFoundException("Content with ID " + id + " not found for deletion");
        }
    }

    // ----------------------------------------------------------------------
    // CORE INTERACTION METHODS
    // ----------------------------------------------------------------------
    
    // 🟢 SECURELY Toggles the like status (LIKE or UNLIKE)
    @Transactional
    public void toggleLike(Long contentId, Long userId) {
        
        Content content = repo.findById(contentId)
                .orElseThrow(() -> new EntityNotFoundException("Content not found with ID: " + contentId));

        Optional<Like> existingLike = likeRepo.findByUserIdAndContentId(userId, contentId);

        if (existingLike.isPresent()) {
            likeRepo.delete(existingLike.get());
            if (content.getLikes() > 0) { 
                content.setLikes(content.getLikes() - 1);
            }
        } else {
            Like newLike = new Like(userId, contentId); 
            likeRepo.save(newLike);
            
            content.setLikes(content.getLikes() + 1);
        }
        
        repo.save(content); 
    }
    
    // ✅ Increment view count
    @Transactional 
    public void incrementViews(Long id) {
        Content content = getById(id); 
        content.setViews(content.getViews() + 1);
        repo.save(content);
    }
}
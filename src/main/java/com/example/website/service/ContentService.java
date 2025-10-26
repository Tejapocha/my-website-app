package com.example.website.service;

import com.example.website.model.Content;
import com.example.website.model.Like;
import com.example.website.model.Comment;
import com.example.website.repository.ContentRepository;
import com.example.website.repository.LikeRepository;
import com.example.website.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ContentService {

    // Declare all fields as private final
    private final ContentRepository contentRepository;
    private final CloudinaryService cloudinaryService;
    private final LikeRepository likeRepository; 
    private final CommentRepository commentRepository;

    // 💡 CHANGE 1: Manual Constructor (Replaces @RequiredArgsConstructor)
    public ContentService(ContentRepository contentRepository, CloudinaryService cloudinaryService,
                          LikeRepository likeRepository, CommentRepository commentRepository) {
        this.contentRepository = contentRepository;
        this.cloudinaryService = cloudinaryService;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
    }

    // --- CRUD METHODS ---

    @Transactional
    public void saveContent(Content content, MultipartFile file) throws IOException {
        // ... (Method body remains the same) ...
        if (file == null || file.isEmpty()) {
            throw new IOException("Cannot save content: Uploaded file is missing or empty.");
        }

        String publicUrl = cloudinaryService.uploadFile(file);
        content.setFilePath(publicUrl);

        content.setUploadDate(LocalDateTime.now());

        String contentType = file.getContentType();
        if (contentType != null) {
            content.setFileType(contentType.startsWith("video") ? "video" :
                                 contentType.startsWith("image") ? "image" : "other");
        } else {
             content.setFileType("unknown");
        }

        if (content.getViews() == null) content.setViews(0);
        if (content.getLikes() == null) content.setLikes(0);

        contentRepository.save(content);
    }

    @Transactional
    public void updateContent(Content updatedContent, MultipartFile file) throws IOException {
        
        Content existingContent = contentRepository.findById(updatedContent.getId())
                .orElseThrow(() -> new EntityNotFoundException("Content not found with ID: " + updatedContent.getId()));

        existingContent.setTitle(updatedContent.getTitle());
        existingContent.setDescription(updatedContent.getDescription());
        existingContent.setTags(updatedContent.getTags());
        
        if (file != null && !file.isEmpty()) {
            String publicUrl = cloudinaryService.uploadFile(file);
            existingContent.setFilePath(publicUrl);

            String contentType = file.getContentType();
            if (contentType != null) {
                existingContent.setFileType(contentType.startsWith("video") ? "video" :
                                         contentType.startsWith("image") ? "image" : "other");
            } else {
                 existingContent.setFileType("unknown");
            }
        }
        
        contentRepository.save(existingContent);
    }

    // --- VIEW & INTERACTION METHODS ---

    public List<Comment> getCommentsByContentId(Long contentId) {
        return commentRepository.findByContent_IdOrderByPostDateDesc(contentId);
    }

    public Content getById(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content not found with ID: " + id));
    }

    @Transactional
    public void delete(Long id) {
        Content content = getById(id);
        contentRepository.delete(content);
    }

    @Transactional
    public void incrementViews(Long contentId) {
        contentRepository.incrementViews(contentId);
    }

    @Transactional
    public int toggleLike(Long contentId, Long userId) {
        Content content = getById(contentId);
        Optional<Like> existingLike = likeRepository.findByContentIdAndUserId(contentId, userId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            content.setLikes(content.getLikes() - 1);
        } else {
            Like newLike = new Like(contentId, userId);
            likeRepository.save(newLike);
            content.setLikes(content.getLikes() + 1);
        }

        contentRepository.save(content); 
        return content.getLikes();
    }

    public List<Long> getLikedContentIds(Long userId) {
        return likeRepository.findContentIdsByUserId(userId);
    }

    public boolean isLikedByUser(Long contentId, Long userId) {
        return likeRepository.findByContentIdAndUserId(contentId, userId).isPresent();
    }

    @Transactional
    public void addComment(Long contentId, String userName, String commentText) {
        Content content = contentRepository.getReferenceById(contentId);

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setUserName(userName);
        comment.setText(commentText);
        comment.setPostDate(LocalDateTime.now());

        commentRepository.save(comment);
    }

    // --- PAGINATION / FILTERING METHODS ---

    /**
     * Retrieves content based on keyword or all content, sorted by date.
     */
    public Page<Content> getPaginated(String keyword, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("uploadDate").descending());

        if (keyword != null && !keyword.isEmpty()) {
            // 💡 FIX 2: Use the newly added Repository method for combined search
            return contentRepository.findByTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(keyword, keyword, pageable);
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
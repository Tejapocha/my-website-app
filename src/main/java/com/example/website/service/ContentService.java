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

    private final ContentRepository contentRepository;
    private final CloudinaryService cloudinaryService;
    private final LikeRepository likeRepository; 
    private final CommentRepository commentRepository;

    public ContentService(ContentRepository contentRepository, CloudinaryService cloudinaryService,
                          LikeRepository likeRepository, CommentRepository commentRepository) {
        this.contentRepository = contentRepository;
        this.cloudinaryService = cloudinaryService;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
    }

    private String getFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            return contentType.startsWith("video") ? "video" :
                   contentType.startsWith("image") ? "image" : "other";
        }
        return "unknown";
    }

    // -----------------------------------------------------------------------------------
    // --- CRUD METHODS ---
    // -----------------------------------------------------------------------------------

    @Transactional
    public void saveContent(Content content, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Cannot save content: Uploaded file is missing or empty.");
        }

        // 1. Upload the main file
        String publicUrl = cloudinaryService.uploadFile(file);
        content.setFilePath(publicUrl);

        // 2. Determine file type
        String fileType = getFileType(file);
        content.setFileType(fileType);

        // 3. ⭐ NEW: Generate and set thumbnail URL for videos ⭐
        if ("video".equals(fileType)) {
            // Cloudinary transformation: replaces /upload/ with /upload/w_400,c_fill,g_auto,pg_1/
            // which tells Cloudinary to extract the first frame (pg_1), resize it to 400px, 
            // and use it as an image URL.
            String thumbnailUrl = generateCloudinaryThumbnailUrl(publicUrl);
            content.setThumbnailUrl(thumbnailUrl);
        } else if ("image".equals(fileType)) {
             // For images, the thumbnail URL can just be the main image URL itself
             content.setThumbnailUrl(publicUrl);
        } else {
             content.setThumbnailUrl(null); // Or a generic placeholder URL
        }

        content.setUploadDate(LocalDateTime.now());
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
            // 1. Upload new file
            String publicUrl = cloudinaryService.uploadFile(file);
            existingContent.setFilePath(publicUrl);

            // 2. Determine new file type
            String fileType = getFileType(file);
            existingContent.setFileType(fileType);
            
            // 3. ⭐ NEW: Update thumbnail URL if file changed ⭐
            if ("video".equals(fileType)) {
                String thumbnailUrl = generateCloudinaryThumbnailUrl(publicUrl);
                existingContent.setThumbnailUrl(thumbnailUrl);
            } else if ("image".equals(fileType)) {
                 existingContent.setThumbnailUrl(publicUrl);
            } else {
                 existingContent.setThumbnailUrl(null);
            }
        }
        
        contentRepository.save(existingContent);
    }
    
    /**
     * Helper to create a thumbnail image URL from a Cloudinary video URL 
     * using URL transformations.
     * @param videoUrl The base public URL of the video.
     * @return The transformed URL pointing to a thumbnail image.
     */
    private String generateCloudinaryThumbnailUrl(String videoUrl) {
        // Example video URL structure: 
        // .../upload/v123456789/my_folder/my_video.mp4
        
        // We inject the transformation: w_400,c_fill,g_auto,pg_1 (width 400, fill, auto gravity, page 1/first frame)
        // We also change the extension to .jpg to treat the result as an image.
        
        String transformation = "/upload/w_400,c_fill,g_auto,pg_1/";
        
        // Find the index of the first segment after /upload/ (e.g., v123456789)
        int uploadIndex = videoUrl.indexOf("/upload/");
        if (uploadIndex == -1) {
            return videoUrl; // Fallback or error
        }
        
        // URL parts: BASE_URL + /upload/ + VERSION_FOLDER/FILE_NAME.EXT
        int versionIndex = videoUrl.indexOf("/", uploadIndex + "/upload/".length());
        
        if (versionIndex == -1) {
             // Structure is unexpected, return original or default
            return videoUrl;
        }

        // BASE_URL + /upload/
        String baseUrlWithUpload = videoUrl.substring(0, uploadIndex) + "/upload/";
        
        // VERSION_FOLDER/FILE_NAME.EXT (or just FILE_NAME.EXT if no version)
        String pathAfterUpload = videoUrl.substring(uploadIndex + "/upload/".length());
        
        // The transformation is applied AFTER the /upload/
        String newUrl = baseUrlWithUpload + transformation + pathAfterUpload;

        // Finally, ensure the extension is .jpg (important for image display)
        int lastDotIndex = newUrl.lastIndexOf('.');
        if (lastDotIndex > newUrl.lastIndexOf('/')) {
             newUrl = newUrl.substring(0, lastDotIndex) + ".jpg";
        } else {
             newUrl += ".jpg";
        }

        return newUrl;
    }


    // -----------------------------------------------------------------------------------
    // --- VIEW & INTERACTION METHODS (UNCHANGED) ---
    // -----------------------------------------------------------------------------------

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

    // -----------------------------------------------------------------------------------
    // --- PAGINATION / FILTERING METHODS (UNCHANGED) ---
    // -----------------------------------------------------------------------------------

    public Page<Content> getPaginated(String keyword, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("uploadDate").descending());

        if (keyword != null && !keyword.isEmpty()) {
            return contentRepository.findByTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            return contentRepository.findAll(pageable);
        }
    }

    public Page<Content> getMostViewedPaginated(int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("views").descending());
        return contentRepository.findAll(pageable);
    }

    public Page<Content> getMostLikedPaginated(int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("likes").descending());
        return contentRepository.findAll(pageable);
    }

    public Page<Content> getVideosByTagPaginated(String tag, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("uploadDate").descending());
        return contentRepository.findByTagsContainingIgnoreCase(tag, pageable);
    }
}
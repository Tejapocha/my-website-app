package com.example.website.controller;

import com.example.website.model.Content;
import com.example.website.service.ContentService;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import com.example.website.model.UserPrincipal; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;

@Controller
public class ContentController {

    @Autowired
    private ContentService service;

    // Folder where uploads will be stored (relative to project root)
    private final String uploadDir = new File("uploads").getAbsolutePath() + File.separator;

    // ✅ Show upload form
    // Requires ADMIN role via SecurityConfig
    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("content", new Content());
        return "upload";
    }

    // ✅ Handle file upload (image/video only)
    // Requires ADMIN role via SecurityConfig
    @PostMapping("/upload")
    public String uploadContent(@ModelAttribute Content content,
                                @RequestParam("file") MultipartFile file) throws IOException {

        if (!file.isEmpty()) {
            String fileName = file.getOriginalFilename();

            // Validate allowed file types
            if (fileName == null || !fileName.matches(".*\\.(png|jpg|jpeg|mp4|mov|webm)$")) {
                throw new IOException("Only PNG, JPG, JPEG, MP4, MOV, and WEBM files are allowed.");
            }

            // Create upload folder if not exists
            File uploadFolder = new File(uploadDir);
            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }

            // Save file
            File dest = new File(uploadDir + fileName); 
            file.transferTo(dest);

            // Detect file type
            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    content.setFileType("image");
                } else if (contentType.startsWith("video/")) {
                    content.setFileType("video");
                } else {
                    throw new IOException("Unsupported file type: " + contentType);
                }
            }

            // Set file path for displaying in UI
            content.setFilePath("/uploads/" + fileName);
        } else {
            throw new IOException("Please select a file to upload.");
        }

        // Default values
        content.setLikes(0);
        content.setViews(0);
        content.setComments("");
        if (content.getTags() == null) content.setTags("");

        service.save(content);
        return "redirect:/dashboard";
    }

    // ✅ Dashboard, Home, and Menu Views (Consolidated mapping)
    // All paths require an authenticated user via SecurityConfig
    @GetMapping({"/", "/dashboard", "/most-viewed", "/most-liked", "/celebrity-videos"})
    public String viewDashboard(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        int pageSize = 10; // show 10 videos per page
        
        // Note: The logic here should dynamically call different service methods 
        // based on the request path (e.g., call service.getMostViewed() for /most-viewed).
        // For simplicity here, we stick to getPaginated, but the service logic should be improved.

        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("contents", service.getPaginated(keyword, page, pageSize));
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalPages", service.getTotalPages(keyword, pageSize));

        return "dashboard";
    }

    // ✅ Like button (Securely handles one like per user via ContentService/LikeRepository)
    @PostMapping("/content/like/{id}")
    public String likeContent(@PathVariable Long id, RedirectAttributes ra) {
        
        // --- AUTHENTICATION/USER ID RETRIEVAL (MANDATORY IN REAL APP) ---
        // TODO: Replace this hardcoded value with the actual authenticated user's ID
        Long userId = 1L; 
        // ----------------------------------------------------------------

        try {
            service.toggleLike(id, userId);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/dashboard";
    }

    // ✅ Increment views when content is played (Called via AJAX)
    @PostMapping("/content/view/{id}")
    public String incrementView(@PathVariable Long id) {
        // Delegate the logic to the service layer for transactional integrity
        service.incrementViews(id); 
        // We redirect to dashboard, although an AJAX call might prefer a 200 OK response.
        return "redirect:/dashboard";
    }

    // ✅ Comment button
    @PostMapping("/content/comment/{id}")
    public String commentContent(@PathVariable Long id, @RequestParam("comment") String comment) {
        Content content = service.getById(id);
        if (content != null && comment != null && !comment.trim().isEmpty()) {
            String existing = content.getComments();
            if (existing == null || existing.isEmpty()) {
                content.setComments(comment);
            } else {
                content.setComments(existing + "\n" + comment);
            }
            service.save(content);
        }
        return "redirect:/dashboard";
    }

    // ✅ Delete content (and file)
    // Requires ADMIN role via SecurityConfig
    @PostMapping("/content/delete/{id}")
    public String deleteContent(@PathVariable Long id) {
        Content content = service.getById(id);

        if (content != null && content.getFilePath() != null) {
            String relativePath = content.getFilePath().replaceFirst("^/uploads/", "");
            File file = new File(uploadDir + relativePath);
            if (file.exists()) {
                file.delete();
            }
        }
     
        service.delete(id);
        return "redirect:/dashboard";
    }
}
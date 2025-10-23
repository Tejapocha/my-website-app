package com.example.website.controller;

import com.example.website.model.Content;
import com.example.website.service.ContentService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ContentController {

    @Autowired
    private ContentService contentService;

    // Hardcoded placeholder user ID for demonstration/testing purposes
    private static final Long PLACEHOLDER_USER_ID = 1L;

    /**
     * Helper to determine the actual user ID.
     */
    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                // IMPORTANT: This assumes the authentication principal name is a parsable Long ID.
                return Long.parseLong(authentication.getName());
            } catch (NumberFormatException e) {
                System.err.println("Authenticated user principal name is not a number. Falling back to placeholder ID.");
            }
        }
        // Fallback ID for non-authenticated users or unparsable authenticated users
        return PLACEHOLDER_USER_ID;
    }


    /**
     * Shows the main dashboard, supporting standard search/pagination AND filtering.
     */
    @GetMapping({"/dashboard", "/"})
    public String showDashboard(Model model,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String filter,
                                @RequestParam(required = false) String tag, // New parameter for category tags
                                Authentication authentication) { 

        Page<Content> contentPage;
        String activeFilterTitle = "Latest Content"; 

        if (keyword != null && !keyword.isEmpty()) {
            // Search takes precedence
            contentPage = contentService.getPaginated(keyword, page, pageSize);
            activeFilterTitle = "Search Results for: '" + keyword + "'";
            model.addAttribute("keyword", keyword);
        } else if (filter != null && !filter.isEmpty()) {
            // Sorting filters
            switch (filter.toLowerCase()) {
                case "best_videos":
                case "most_liked":
                    contentPage = contentService.getMostLikedPaginated(page, pageSize);
                    activeFilterTitle = "Best Videos (Most Liked)";
                    break;
                case "most_viewed":
                    contentPage = contentService.getMostViewedPaginated(page, pageSize);
                    activeFilterTitle = "Most Viewed Videos";
                    break;
                default:
                    contentPage = contentService.getPaginated(null, page, pageSize);
            }
            model.addAttribute("filter", filter);
        } else if (tag != null && !tag.isEmpty()) {
            // Category/Tag filters
            switch (tag.toLowerCase()) {
                case "celebrity":
                    contentPage = contentService.getVideosByTagPaginated("celebrity", page, pageSize);
                    activeFilterTitle = "Celebrity Videos";
                    break;
                case "homemade":
                    contentPage = contentService.getVideosByTagPaginated("homemade", page, pageSize);
                    activeFilterTitle = "Homemade Videos";
                    break;
                case "college":
                    contentPage = contentService.getVideosByTagPaginated("college", page, pageSize);
                    activeFilterTitle = "College Videos";
                    break;
                default:
                    contentPage = contentService.getPaginated(null, page, pageSize);
            }
            model.addAttribute("tag", tag);
        } else {
            // Default latest content
            contentPage = contentService.getPaginated(null, page, pageSize);
        }
        
        Long currentUserId = getUserId(authentication);
        List<Long> likedContentIds = contentPage.getContent().stream()
                .filter(content -> contentService.isLikedByUser(content.getId(), currentUserId))
                .map(Content::getId)
                .collect(Collectors.toList());
        
        model.addAttribute("likedContentIds", likedContentIds);

        model.addAttribute("contents", contentPage.getContent());
        model.addAttribute("currentPage", contentPage.getNumber() + 1); 
        model.addAttribute("totalPages", contentPage.getTotalPages());
        model.addAttribute("activeFilterTitle", activeFilterTitle);

        return "dashboard";
    }


    /**
     * Handles the GET request to display a single content detail page.
     */
    @GetMapping("/view/{id}")
    public String showContentDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            Content content = contentService.getById(id);
            model.addAttribute("content", content);
            
            // Increment view count immediately when the dedicated page is loaded
            contentService.incrementViews(id); 

            // Determine if the current user has liked this specific content
            Long currentUserId = getUserId(authentication); 
            model.addAttribute("isLiked", contentService.isLikedByUser(id, currentUserId));

            return "content-detail"; 
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Error: Content not found.");
            return "redirect:/dashboard"; 
        }
    }


    // --- UPLOAD METHODS ---

    /**
     * Handles the GET request to display the content upload form.
     */
    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("content", new Content());
        return "upload"; // Assuming you have an 'upload.html' Thymeleaf template
    }

    /**
     * Handles the POST request for submitting new content.
     */
    @PostMapping("/upload")
    public String uploadContent(@ModelAttribute Content content,
                                @RequestParam("file") MultipartFile file,
                                RedirectAttributes redirectAttributes) {
        try {
            contentService.saveContent(content, file);
            redirectAttributes.addFlashAttribute("message", "Content uploaded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload content: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
    // --- INTERACTION METHODS ---

    /**
     * Deletes content by ID.
     */
    @PostMapping("/delete/{id}")
    public String deleteContent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            contentService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Content deleted successfully!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting content: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    /**
     * Toggles the like status (LIKE or UNLIKE) and returns JSON for AJAX update.
     * Used by content-detail.html for non-reloading like updates.
     */
    @PostMapping("/like/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication); 
        Map<String, Object> response = new HashMap<>();

        try {
            // The service method returns the new total like count
            int newLikes = contentService.toggleLike(id, userId);
            
            response.put("success", true);
            response.put("newLikes", newLikes);
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("Error toggling like: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Failed to toggle like status.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles comment submission for a piece of content.
     */
    @PostMapping("/comment/{id}")
    public String postComment(@PathVariable Long id,
                              @RequestParam("comment") String comment,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {

        String userName;
        if (authentication != null && authentication.isAuthenticated()) {
            userName = authentication.getName(); 
        } else {
            userName = "Anonymous Viewer";
        }

        try {
            contentService.addComment(id, userName, comment);
            redirectAttributes.addFlashAttribute("message", "Comment posted successfully!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Error: Content not found.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error posting comment: " + e.getMessage());
            System.err.println("Error posting comment: " + e.getMessage());
        }

        // Redirect back to the content detail page if this was a comment submission
        return "redirect:/view/" + id;
    }

    /**
     * Increments the view count (Used by AJAX in dashboard.html).
     */
    @PostMapping("/view/{id}")
    @ResponseBody
    public String incrementView(@PathVariable Long id) {
        try {
            contentService.incrementViews(id);
            return "{\"success\": true}";
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Handles the GET request for the About page.
     */
    @GetMapping("/about")
    public String showAboutPage() {
        return "about"; // Maps to about.html
    }
}
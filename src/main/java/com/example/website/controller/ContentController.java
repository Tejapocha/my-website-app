package com.example.website.controller;

import com.example.website.model.Content;
import com.example.website.service.ContentService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ContentController {

    @Autowired
    private ContentService contentService;

    // Hardcoded placeholder user ID for demonstration/testing purposes
    // NOTE: In a real application, this MUST be replaced by a dynamically fetched authenticated user ID.
    private static final Long PLACEHOLDER_USER_ID = 1L;

    /**
     * Shows the main dashboard, supporting standard search/pagination AND filtering by
     * 'most_liked', 'most_viewed', or 'celebrity'.
     */
    @GetMapping({"/dashboard", "/"})
    public String showDashboard(Model model,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String filter,
                                Authentication authentication) { // Added Authentication

        Page<Content> contentPage;
        String activeFilterTitle = "Latest Content"; // Default title

        // 1. Logic to determine which service method to call based on the 'filter' parameter
        if (keyword != null && !keyword.isEmpty()) {
            contentPage = contentService.getPaginated(keyword, page, pageSize);
            activeFilterTitle = "Search Results for: '" + keyword + "'";
            model.addAttribute("keyword", keyword);
        } else if (filter != null && !filter.isEmpty()) {
            switch (filter.toLowerCase()) {
                case "most_liked":
                    contentPage = contentService.getMostLikedPaginated(page, pageSize);
                    activeFilterTitle = "Most Liked Videos";
                    break;
                case "most_viewed":
                    contentPage = contentService.getMostViewedPaginated(page, pageSize);
                    activeFilterTitle = "Most Viewed Videos";
                    break;
                case "celebrity":
                    contentPage = contentService.getCelebrityVideosPaginated(page, pageSize);
                    activeFilterTitle = "Celebrity Videos";
                    break;
                default:
                    contentPage = contentService.getPaginated(null, page, pageSize);
            }
            model.addAttribute("filter", filter);
        } else {
            contentPage = contentService.getPaginated(null, page, pageSize);
        }
        
        // 2. Determine which content is liked by the current user (to persist the visual state)
        List<Long> likedContentIds = contentPage.getContent().stream()
                .filter(content -> contentService.isLikedByUser(content.getId(), PLACEHOLDER_USER_ID))
                .map(Content::getId)
                .collect(Collectors.toList());
        
        model.addAttribute("likedContentIds", likedContentIds);

        // 3. Populate the model
        model.addAttribute("contents", contentPage.getContent());
        model.addAttribute("currentPage", contentPage.getNumber() + 1); // Convert 0-based index to 1-based
        model.addAttribute("totalPages", contentPage.getTotalPages());
        model.addAttribute("activeFilterTitle", activeFilterTitle);
        // Note: 'keyword' is only added if present, preventing it from interfering with filters.

        return "dashboard";
    }


    /**
     * Handles the GET request to display a single content detail page.
     * This method is changed to redirect back to the main dashboard listing.
     * This is useful if the user tries to navigate directly to the view URL.
     */
    @GetMapping("/view/{id}")
    public String showContentDetail(@PathVariable Long id) {
        // The unique link should redirect to the dashboard with the content ID as the keyword
        return "redirect:/dashboard?keyword=" + id;
    }


    // --- UPLOAD METHODS ---

    /**
     * Handles the GET request to display the content upload form.
     * Accessible only by ADMIN users via Spring Security.
     */
    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("content", new Content());
        return "upload"; // Assuming you have an 'upload.html' Thymeleaf template
    }

    /**
     * Handles the POST request for submitting new content.
     * Accessible only by ADMIN users via Spring Security.
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
     * Accessible only by ADMIN users via Spring Security.
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
     * Toggles the like status (LIKE or UNLIKE).
     * This version is updated to return a JSON response for AJAX updates.
     */
    @PostMapping("/like/{id}")
    @ResponseBody // FIX 1: Returns data, not a view/redirect
    public String toggleLike(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // Return an unauthorized error message as JSON
            return "{\"success\": false, \"error\": \"Login required to like content.\"}";
        }
        
        Long userId = PLACEHOLDER_USER_ID; 

        try {
            // contentService.toggleLike now returns the new count
            int newLikes = contentService.toggleLike(id, userId);
            // FIX 2: Return success and the new like count as JSON
            return "{\"success\": true, \"newLikes\": " + newLikes + "}";
        } catch (Exception e) {
            // Return error message as JSON
            return "{\"success\": false, \"error\": \"Error toggling like: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Handles comment submission for a piece of content.
     * This method now allows unauthenticated users to comment as "Anonymous Viewer."
     */
    @PostMapping("/comment/{id}")
    public String postComment(@PathVariable Long id,
                              @RequestParam("comment") String comment,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {

        // Determine the username. Authentication is NOT required for commenting.
        String userName;
        if (authentication != null && authentication.isAuthenticated()) {
            // Use the logged-in username
            userName = authentication.getName(); 
        } else {
            // Use a default name for viewers
            userName = "Anonymous Viewer";
        }

        try {
            // Assumes ContentService has an addComment method that updates the Content entity
            contentService.addComment(id, userName, comment);
            redirectAttributes.addFlashAttribute("message", "Comment posted successfully!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Error: Content not found.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error posting comment: " + e.getMessage());
            System.err.println("Error posting comment: " + e.getMessage());
        }

        // Redirect back to the dashboard to show the updated content list (and implicitly the new comment)
        return "redirect:/dashboard";
    }

    /**
     * Increments the view count (Used by AJAX in dashboard.html).
     * The method is annotated with @ResponseBody to return a simple JSON response.
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
}

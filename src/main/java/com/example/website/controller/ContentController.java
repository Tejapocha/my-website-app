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
                                Authentication authentication) { 

        Page<Content> contentPage;
        String activeFilterTitle = "Latest Content"; 

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
     * * FIX: This method is now updated to fetch the specific content and render a 
     * dedicated detail template (content-detail) instead of redirecting.
     */
    @GetMapping("/view/{id}")
    public String showContentDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Content content = contentService.getById(id);
            model.addAttribute("content", content);
            
            // Increment view count immediately when the dedicated page is loaded
            contentService.incrementViews(id); 

            // Determine if the current (or placeholder) user has liked this specific content
            Long currentUserId = getUserId(null); // Passing null as Authentication since this method doesn't need full auth check
            model.addAttribute("isLiked", contentService.isLikedByUser(id, currentUserId));

            // Return the name of the new template
            return "content-detail"; 
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Error: Content not found.");
            return "redirect:/dashboard"; 
        }
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
     */
    @PostMapping("/like/{id}")
    @ResponseBody 
    public String toggleLike(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication); 

        try {
            int newLikes = contentService.toggleLike(id, userId);
            return "{\"success\": true, \"newLikes\": " + newLikes + "}";
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"Error toggling like: " + e.getMessage() + "\"}";
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
}

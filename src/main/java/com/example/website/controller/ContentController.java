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

@Controller
public class ContentController {

    @Autowired
    private ContentService contentService;

    // --- DASHBOARD AND LISTING METHODS ---

    @GetMapping({"/dashboard", "/"})
    public String showDashboard(Model model,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) String keyword) {
        
        // currentPage is 0-indexed in Page object, but we use 1-based index in view
        Page<Content> contentPage = contentService.getPaginated(keyword, page, pageSize);
        
        model.addAttribute("contents", contentPage.getContent());
        model.addAttribute("currentPage", contentPage.getNumber()); // 0-based index
        model.addAttribute("totalPages", contentPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "dashboard";
    }
    
    /**
     * Handles the GET request to display a single content detail page.
     * This method is changed to redirect back to the main dashboard listing.
     * This is useful if the user tries to navigate directly to the view URL.
     */
    @GetMapping("/view/{id}")
    public String showContentDetail(@PathVariable Long id) {
        // The AJAX call handles the view count increment via POST, so the GET request 
        // can simply redirect the user back to the main content list.
        return "redirect:/dashboard";
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
     * This version uses a full page redirect after the like operation.
     */
    @PostMapping("/like/{id}")
    public String toggleLike(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login"; // Force login if not authenticated
        }
        // NOTE: You must replace '1L' with actual logic to extract the user's ID
        // from the 'authentication' object in a real application.
        Long userId = 1L; // Placeholder for authenticated user ID

        try {
            contentService.toggleLike(id, userId); 
        } catch (Exception e) {
            System.err.println("Error toggling like: " + e.getMessage());
        }
        // Redirect back to the dashboard to refresh the content list and like count
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

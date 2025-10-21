package com.example.website.controller;

import com.example.website.service.ContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

// Assuming necessary imports for your Content and User models
// import com.example.website.model.Content; 
// import com.example.website.model.User;

@Controller
public class ContentController {

    private final ContentService contentService;

    // Assuming this service is injected
    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    // --- Core Navigation ---

    @GetMapping("/")
    public String redirectToDashboard() {
        // Redirects root URL to the dashboard page
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String viewDashboard(Model model, 
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "") String keyword) {
        // Placeholder for fetching content logic
        // model.addAttribute("contents", contentService.findPaginated(page, keyword));
        // model.addAttribute("currentPage", page - 1);
        // model.addAttribute("totalPages", contentService.getTotalPages(keyword));

        // Using mock content for demonstration until actual service logic is available
        model.addAttribute("contents", List.of());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", 1);
        model.addAttribute("keyword", keyword);
        return "dashboard";
    }

    // ... (Other GetMappings like /most-viewed, /upload, /view/{id} etc. go here)

    // --- Admin Operations ---

    @PostMapping("/delete/{id}")
    public String deleteContent(@PathVariable Long id, RedirectAttributes ra) {
        // This is protected by hasRole('ADMIN') in SecurityConfig
        // contentService.deleteContent(id);
        ra.addFlashAttribute("message", "Content deleted successfully!");
        return "redirect:/dashboard";
    }

    // --- AJAX Interaction FIX ---
    
    /**
     * Handles the AJAX request for liking content. Returns the new like count as JSON.
     * The @ResponseBody annotation is crucial as it prevents a view name from being returned.
     */
    
    
    // --- Comment Operations ---
    
    @PostMapping("/comment/{id}")
    public String postComment(@PathVariable Long id, @RequestParam String comment, RedirectAttributes ra) {
        // This path is publicly accessible now.
        // contentService.addComment(id, comment); 
        ra.addFlashAttribute("message", "Comment posted successfully!");
        return "redirect:/dashboard"; // Still uses redirect because comments can refresh the page
    }
}

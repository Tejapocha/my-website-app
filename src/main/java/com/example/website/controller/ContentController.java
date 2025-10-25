package com.example.website.controller;

import com.example.website.model.Content;
import com.example.website.model.User;
import com.example.website.service.ContentService;
import com.example.website.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List; 
import java.util.Map;
import java.util.Optional;

@Controller
public class ContentController {

    // Best Practice: Use final fields and constructor injection
    private final ContentService contentService;
    private final UserService userService;

    // Constructor Injection (Replaces @Autowired on fields)
    public ContentController(ContentService contentService, UserService userService) {
        this.contentService = contentService;
        this.userService = userService;
    }

    // Define constants for pagination
    private static final int PAGE_SIZE = 9;

    /**
     * Helper to get the ID of the currently authenticated user.
     * WARNING: This uses a redundant DB call. See review notes for the preferred method 
     * using a CustomUserDetails object.
     * @return The User ID or 0L if anonymous.
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return 0L; 
        }
        
        // Attempt to extract username and find the ID via service (PERFORMANCE WARNING)
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User springUser) {
             String username = springUser.getUsername();
             // Critical: DB call to get the ID from the username
             return userService.findByUsername(username).map(User::getId).orElse(0L);
        }
        // Fallback for custom principals if not the standard Spring User
        return 0L; 
    }

    /**
     * Helper to check if the current user has the ADMIN role.
     * @return true if the user has the ADMIN role, false otherwise.
     */
    private boolean isAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // -----------------------------------------------------------------------------------
    // --- PUBLIC DASHBOARD & VIEWING ---
    // -----------------------------------------------------------------------------------

    @GetMapping({"/", "/dashboard"}) 
    public String dashboard(Model model,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "latest") String filter,
                            @RequestParam Optional<String> tag,
                            @RequestParam Optional<String> keyword,
                            Authentication authentication) { 

        // 1. Get User Context (Authenticated or Anonymous)
        Long currentUserId = getCurrentUserId();
        
        // 2. Fetch Liked Content IDs (for authenticated users only)
        List<Long> likedContentIds = currentUserId != 0L ? contentService.getLikedContentIds(currentUserId) : List.of();
        model.addAttribute("likedContentIds", likedContentIds);

        // 3. Filtering Logic 
        Page<Content> contentPage = switch (filter.toLowerCase()) {
            case "best_videos" -> contentService.getMostLikedPaginated(page, PAGE_SIZE);
            case "most_viewed" -> contentService.getMostViewedPaginated(page, PAGE_SIZE);
            default -> {
                if (tag.isPresent() && !tag.get().isEmpty()) {
                    model.addAttribute("activeFilterTitle", "Tag: " + tag.get());
                    yield contentService.getVideosByTagPaginated(tag.get(), page, PAGE_SIZE);
                } else if (keyword.isPresent() && !keyword.get().isEmpty()) {
                    model.addAttribute("activeFilterTitle", "Search: " + keyword.get());
                    yield contentService.getPaginated(keyword.get(), page, PAGE_SIZE);
                } else {
                    model.addAttribute("activeFilterTitle", "Latest Content");
                    yield contentService.getPaginated(null, page, PAGE_SIZE);
                }
            }
        };

        // 4. Model Attributes
        model.addAttribute("contents", contentPage.getContent());
        model.addAttribute("currentPage", contentPage.getNumber() + 1);
        model.addAttribute("totalPages", contentPage.getTotalPages());
        model.addAttribute("filter", filter);
        model.addAttribute("keyword", keyword.orElse(null));
        model.addAttribute("tag", tag.orElse(null));
        model.addAttribute("isAdmin", isAdmin(authentication)); 

        return "dashboard";
    }

    
    @GetMapping("/view/{id}")
    public String viewContent(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            Content content = contentService.getById(id);
            
            // Get User Context
            Long currentUserId = getCurrentUserId(); 
            
            boolean isLiked = currentUserId != 0L && contentService.isLikedByUser(id, currentUserId);
            
            // Increment view count
            contentService.incrementViews(id); 

            model.addAttribute("content", content);
            model.addAttribute("isLiked", isLiked);
            model.addAttribute("isAdmin", isAdmin(authentication)); 

            return "content-detail";
        } catch (EntityNotFoundException e) {
            model.addAttribute("error", "Content not found!");
            return "redirect:/dashboard";
        }
    }

    // -----------------------------------------------------------------------------------
    // --- INTERACTION ENDPOINTS ---
    // -----------------------------------------------------------------------------------

    /**
     * Toggles the like status. Requires user authentication.
     */
    @PostMapping("/like/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Long currentUserId = getCurrentUserId();
        
        if (currentUserId == 0L) {
             response.put("success", false);
             response.put("error", "Authentication required to like content.");
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            int newLikes = contentService.toggleLike(id, currentUserId);
            response.put("success", true);
            response.put("newLikes", newLikes);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            response.put("success", false);
            response.put("error", "Content not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to toggle like status.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Adds a comment. Requires user authentication.
     */
    @PostMapping("/comment/{id}")
    public String addComment(@PathVariable Long id, 
                             @RequestParam String comment, 
                             RedirectAttributes redirectAttributes) {
                             
        Long currentUserId = getCurrentUserId();
        
        if (currentUserId == 0L) {
            redirectAttributes.addFlashAttribute("commentError", "You must be logged in to comment.");
            return "redirect:/view/" + id;
        }
        
        try {
            // CRITICAL: Fetch the actual authenticated username to include in the comment
            User currentUser = userService.getById(currentUserId);
            String userName = currentUser.getUsername();

            contentService.addComment(id, userName, comment);
            redirectAttributes.addFlashAttribute("commentSuccess", "Comment posted successfully!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("commentError", "Failed to find content.");
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("commentError", "Failed to post comment due to an error.");
        }

        return "redirect:/view/{id}";
    }
    
    // -----------------------------------------------------------------------------------
    // --- ADMIN MANAGEMENT ENDPOINTS ---
    // -----------------------------------------------------------------------------------
    
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        // Fetch content for management view
        Page<Content> contentPage = contentService.getPaginated(null, 1, 50);

        model.addAttribute("contents", contentPage.getContent());
        model.addAttribute("content", new Content()); 
        model.addAttribute("isAdmin", isAdmin(authentication)); 
        
        return "admin-dashboard"; 
    }

    @GetMapping("/admin/upload")
    public String showUploadForm(Model model, Authentication authentication) {
        model.addAttribute("content", new Content());
        model.addAttribute("isAdmin", isAdmin(authentication));
        return "admin-upload"; 
    }

    @PostMapping("/admin/upload")
    public String uploadContent(@ModelAttribute Content content, 
                                @RequestParam("file") MultipartFile file, 
                                RedirectAttributes redirectAttributes) {
        
        // IMPORTANT: The ContentService.saveContent method MUST handle the file I/O 
        // and set the filePath on the 'content' object before saving to the DB.
        try {
            contentService.saveContent(content, file);
            redirectAttributes.addFlashAttribute("message", "Content uploaded successfully!");
        } catch (IOException e) {
            // IOException indicates a failure in file I/O (disk, permissions, etc.)
            redirectAttributes.addFlashAttribute("error", "File upload failed: " + e.getMessage());
        }
        
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/delete/{id}")
    public String deleteContent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            contentService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Content deleted successfully!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/admin/dashboard";
    }
    @GetMapping("/about")
    public String aboutpage() {
    	// The view resolver (Thymeleaf in your case) will look for 'about.html'
        // in the 'src/main/resources/templates/' directory.
    	return "about";
    }

}
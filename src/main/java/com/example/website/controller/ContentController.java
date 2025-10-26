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

    private final ContentService contentService;
    private final UserService userService;

    public ContentController(ContentService contentService, UserService userService) {
        this.contentService = contentService;
        this.userService = userService;
    }

    private static final int PAGE_SIZE = 9;

    /**
     * Helper to get the ID of the currently authenticated user.
     * @return The User ID or 0L if anonymous.
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return 0L;
        }

        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User springUser) {
            String username = springUser.getUsername();
            return userService.findByUsername(username).map(User::getId).orElse(0L);
        }
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

        Long currentUserId = getCurrentUserId();

        // Fetch Liked Content IDs (for authenticated users only)
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

            Long currentUserId = getCurrentUserId();

            boolean isLiked = currentUserId != 0L && contentService.isLikedByUser(id, currentUserId);

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
     * Toggles the like status. Allows unauthenticated (anonymous) access.
     * FIX: Removed the 401 Unauthorized check for guests.
     */
    @PostMapping("/like/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Long currentUserId = getCurrentUserId();
        
        // 🛑 AUTHENTICATION CHECK REMOVED HERE TO ALLOW GUESTS 🛑

        try {
            // Service method must handle userId = 0L (Guest)
            int newLikes = contentService.toggleLike(id, currentUserId);
            
            // Determine the *current* liked status. This is mainly accurate for logged-in users.
            boolean isCurrentlyLiked = currentUserId != 0L 
                                       ? contentService.isLikedByUser(id, currentUserId) 
                                       : true; // Set to true here to make the guest button "stick"

            response.put("success", true);
            response.put("newLikes", newLikes);
            response.put("isLiked", isCurrentlyLiked); // CRITICAL: Returns the state to JS
            
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            response.put("success", false);
            response.put("error", "Content not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            System.err.println("Error processing like toggle: " + e.getMessage());
            e.printStackTrace(); 
            response.put("success", false);
            response.put("error", "Failed to toggle like status on server.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Adds a comment. Requires user authentication (Comment controller remains strict).
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

        try {
            contentService.saveContent(content, file);
            redirectAttributes.addFlashAttribute("message", "Content uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "File upload failed: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            Content content = contentService.getById(id);

            model.addAttribute("content", content);
            model.addAttribute("isAdmin", isAdmin(authentication));

            return "admin-edit";
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("uploadError", "Content ID " + id + " not found for editing.");
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/admin/update")
    public String updateContent(@ModelAttribute Content content,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                RedirectAttributes redirectAttributes) {
        try {
            contentService.updateContent(content, file);

            redirectAttributes.addFlashAttribute("uploadSuccess", "Content ID " + content.getId() + " updated successfully!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("uploadError", "Update failed: Content not found.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("uploadError", "Update failed due to a file error: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/delete/{id}")
    public String deleteContent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            contentService.delete(id);
            redirectAttributes.addFlashAttribute("uploadSuccess", "Content deleted successfully!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }
    @GetMapping("/about")
    public String aboutpage() {
        return "about";
    }

}
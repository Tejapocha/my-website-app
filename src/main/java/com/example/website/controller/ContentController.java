package com.example.website.controller;

import com.example.website.model.Content;
import com.example.website.service.CloudinaryService;
import com.example.website.service.ContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/") // Class-level prefix to avoid collisions
public class ContentController {

    @Autowired
    private ContentService contentService;

    @Autowired
    private CloudinaryService cloudinaryService;

    // ------------------- Upload -------------------
    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("content", new Content());
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadContent(@ModelAttribute Content content,
                                @RequestParam("file") MultipartFile file,
                                Model model) {

        if (file.isEmpty()) {
            model.addAttribute("errorMessage", "Please select a file to upload.");
            return "upload";
        }

        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.matches(".*\\.(png|jpg|jpeg|mp4|mov|webm)$")) {
                model.addAttribute("errorMessage", "Only PNG, JPG, JPEG, MP4, MOV, and WEBM files are allowed.");
                return "upload";
            }

            String url = cloudinaryService.uploadFile(file);
            content.setFilePath(url);

            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.startsWith("image/")) content.setFileType("image");
                else if (contentType.startsWith("video/")) content.setFileType("video");
                else {
                    model.addAttribute("errorMessage", "Unsupported file type: " + contentType);
                    return "upload";
                }
            }

            content.setLikes(0);
            content.setViews(0);
            content.setComments("");
            if (content.getTags() == null) content.setTags("");

            contentService.saveContent(content, file);

            model.addAttribute("successMessage", "File uploaded successfully!");
            return "upload";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Upload failed: " + e.getMessage());
            return "upload";
        }
    }

    // ------------------- Dashboard / Home -------------------
    @GetMapping({"/dashboard", "/most-viewed", "/most-liked", "/celebrity-videos"})
    public String viewDashboard(@RequestParam(value = "page", defaultValue = "1") int page,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                Model model) {

        int pageSize = 10;

        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("contents", contentService.getPaginated(keyword, page, pageSize));
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalPages", contentService.getTotalPages(keyword, pageSize));

        return "dashboard";
    }

    // ------------------- Like -------------------
    @PostMapping("/like/{id}")
    public String likeContent(@PathVariable Long id, RedirectAttributes ra) {
        Long userId = 1L; // Replace with actual authenticated user
        try {
            contentService.toggleLike(id, userId);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/content/dashboard";
    }

    // ------------------- View -------------------
    @PostMapping("/view/{id}")
    public String incrementView(@PathVariable Long id) {
        contentService.incrementViews(id);
        return "redirect:/content/dashboard";
    }

    // ------------------- Comment -------------------
    @PostMapping("/comment/{id}")
    public String commentContent(@PathVariable Long id, @RequestParam("comment") String comment) {
        Content content = contentService.getById(id);
        if (comment != null && !comment.trim().isEmpty()) {
            String existing = content.getComments();
            content.setComments((existing == null || existing.isEmpty()) ? comment : existing + "\n" + comment);
            contentService.saveContent(content, null); // file = null because only updating metadata
        }
        return "redirect:/content/dashboard";
    }

    // ------------------- Delete -------------------
    @PostMapping("/delete/{id}")
    public String deleteContent(@PathVariable Long id) {
        contentService.delete(id);
        return "redirect:/content/dashboard";
    }
}

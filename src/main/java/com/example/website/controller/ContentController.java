package com.example.website.controller;

import com.example.website.model.Content;
import com.example.website.service.ContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Controller
public class ContentController {

    @Autowired
    private ContentService service;

    // Folder where uploads will be stored (relative to project root)
    private final String uploadDir = new File("uploads").getAbsolutePath() + File.separator;

    // ✅ Temporary in-memory like tracking (user -> liked content IDs)
    // Ideally, move this to DB using a Like table
    private final Set<String> likedRecords = new HashSet<>();

    // ✅ Show upload form
    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("content", new Content());
        return "upload";
    }

    // ✅ Handle file upload (image/video only)
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

    // ✅ Dashboard with pagination + search
    @GetMapping("/dashboard")
    public String viewDashboard(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        int pageSize = 10; // show 10 videos per page
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("contents", service.getPaginated(keyword, page, pageSize));
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalPages", service.getTotalPages(keyword, pageSize));

        return "dashboard";
    }

    // ✅ Like button (only once per user session)
    @PostMapping("/content/like/{id}")
    public String likeContent(@PathVariable Long id, @RequestHeader(value = "X-User", required = false) String user) {
        if (user == null || user.isEmpty()) {
            user = "guest"; // fallback for non-logged users
        }
        String recordKey = user + "-" + id;
        if (!likedRecords.contains(recordKey)) {
            Content content = service.getById(id);
            if (content != null) {
                content.setLikes(content.getLikes() + 1);
                service.save(content);
                likedRecords.add(recordKey);
            }
        }
        return "redirect:/dashboard";
    }

    // ✅ Increment views when video is played
    @PostMapping("/content/view/{id}")
    public String incrementView(@PathVariable Long id) {
        Content content = service.getById(id);
        if (content != null) {
            content.setViews(content.getViews() + 1);
            service.save(content);
        }
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
    @PostMapping("/content/delete/{id}")
    public String deleteContent(@PathVariable Long id) {
        Content content = service.getById(id);

        if (content != null && content.getFilePath() != null) {
            String relativePath = content.getFilePath().replaceFirst("^/+", "");
            File file = new File(relativePath);
            if (file.exists()) {
                file.delete();
            }
        }

        service.delete(id);
        return "redirect:/dashboard";
    }
}

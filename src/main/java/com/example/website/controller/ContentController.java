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

@Controller
public class ContentController {
	
    @Autowired
    private ContentService service;

    // Folder where uploads will be stored (relative to project root)
    private final String uploadDir = new File("uploads").getAbsolutePath() + File.separator;

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

            // ✅ Validate allowed file types
            if (fileName == null || !fileName.matches(".*\\.(png|jpg|jpeg|mp4|mov|webm)$")) {
                throw new IOException("Only PNG, JPG, JPEG, MP4, MOV, and WEBM files are allowed.");
            }

            // ✅ Create upload folder if not exists
            File uploadFolder = new File(uploadDir);
            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }

            // ✅ Save file
            File dest = new File(uploadDir + fileName);
            file.transferTo(dest);

            // ✅ Detect file type
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

            // ✅ Set file path for displaying in UI
            content.setFilePath("/uploads/" + fileName);
        } else {
            throw new IOException("Please select a file to upload.");
        }

        // ✅ Initialize default like & comment values
        content.setLikes(0);
        content.setComments(""); // empty initially

        // ✅ Save in DB
        service.save(content);
        return "redirect:/dashboard";
    }

    // ✅ Dashboard + Search feature
    @GetMapping("/dashboard")
    public String viewDashboard(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("contents", service.search(keyword));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("contents", service.getAll());
        }
        return "dashboard";
    }

    // ✅ Like button
    @PostMapping("/content/like/{id}")
    public String likeContent(@PathVariable Long id) {
        Content content = service.getById(id);
        if (content != null) {
            content.setLikes(content.getLikes() + 1);
            service.save(content);
        }
        return "redirect:/dashboard";
    }

    // ✅ Comment button (append text to comments field)
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

        // Delete the file from storage
        if (content != null && content.getFilePath() != null) {
            String relativePath = content.getFilePath().replaceFirst("^/+", ""); // remove leading /
            File file = new File(relativePath);
            if (file.exists()) {
                file.delete();
            }
        }

        // Delete content from database
        service.delete(id);
        return "redirect:/dashboard";
    }
}



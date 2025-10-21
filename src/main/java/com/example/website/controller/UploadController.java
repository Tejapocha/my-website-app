package com.example.website.controller;

import com.example.website.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class UploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload-file")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        try {
            String imageUrl = cloudinaryService.uploadFile(file);
            model.addAttribute("url", imageUrl);
            // Optionally save URL in DB
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "dashboard";
    }
}

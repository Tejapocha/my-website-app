package com.example.website.service;

import com.example.website.model.Content;
import com.example.website.repository.ContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ContentService {

    @Autowired
    private ContentRepository repo;

    // ✅ Save or update content
    public Content save(Content content) {
        return repo.save(content);
    }

    // ✅ Get all uploaded content
    public List<Content> getAll() {
        return repo.findAll();
    }

    // ✅ Search by title or description (case-insensitive)
    public List<Content> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return repo.findAll(); // show all when no keyword
        }
        return repo.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
    }

    // ✅ Delete content by ID
    public void delete(Long id) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
        } else {
            throw new RuntimeException("Content with ID " + id + " not found for deletion");
        }
    }

    // ✅ Get content by ID safely
    public Content getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Content not found with ID: " + id));
    }
}

package com.example.website.service;

import com.example.website.model.Content;
import com.example.website.repository.ContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class ContentService {

    @Autowired
    private ContentRepository repo;

    // ✅ Save or update content
    public Content save(Content content) {
        return repo.save(content);
    }

    // ✅ Get all uploaded content (no pagination)
    public List<Content> getAll() {
        return repo.findAll();
    }

    // ✅ Pagination support (10 per page)
    public Page<Content> getPaginatedContents(int page) {
        return repo.findAll(PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "id")));
    }
    //✅ Get paginated + searchable results
    public Page<Content> getPaginated(String keyword, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        if (keyword == null || keyword.trim().isEmpty()) {
            return repo.findAll(pageable);
        } else {
            return repo.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable);
        }
    }

    // ✅ Get total pages for pagination
    public int getTotalPages(String keyword, int pageSize) {
        long totalRecords;
        if (keyword == null || keyword.trim().isEmpty()) {
            totalRecords = repo.count();
        } else {
            totalRecords = repo.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
        }
        return (int) Math.ceil((double) totalRecords / pageSize);
    }
    // ✅ Get most liked content
    public List<Content> getMostLiked() {
        return repo.findTop10ByOrderByLikesDesc();
    }

    // ✅ Get most viewed content
    public List<Content> getMostViewed() {
        return repo.findTop10ByOrderByViewsDesc();
    }

    // ✅ Search by title or description
    public List<Content> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return repo.findAll();
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

    // ✅ Increment view count
    public void incrementViews(Long id) {
        Content content = getById(id);
        content.setViews(content.getViews() + 1);
        repo.save(content);
    }
}

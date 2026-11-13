package com.example.lms.course;

import com.example.lms.auth.AuthException;
import com.example.lms.common.PageMeta;
import com.example.lms.common.PageResponse;
import tools.jackson.databind.JsonNode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseCategoryService {

    private final CourseCategoryRepository categoryRepository;

    public CourseCategoryService(CourseCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategorySummary> listActive() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(CategorySummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCategory> listAll(int page, int size) {
        var categories = categoryRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name").and(Sort.by("id"))));
        return new PageResponse<>(
                categories.map(AdminCategory::from).getContent(),
                new PageMeta(
                        categories.getNumber(),
                        categories.getSize(),
                        categories.getTotalElements(),
                        categories.getTotalPages()));
    }

    @Transactional
    public AdminCategory create(CreateCategoryRequest request) {
        String name = request.name().trim();
        String slug = request.slug().trim();
        if (name.isEmpty() || slug.isEmpty()) {
            throw invalidCategory();
        }
        if (categoryRepository.existsBySlug(slug)) {
            throw categoryConflict();
        }
        return AdminCategory.from(categoryRepository.save(new CourseCategory(
                name,
                slug,
                request.description() == null ? null : request.description().trim())));
    }

    @Transactional
    public AdminCategory update(Long categoryId, UpdateCategoryRequest request) {
        CourseCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AuthException(
                        "CATEGORY_NOT_FOUND", "Không tìm thấy danh mục.", HttpStatus.NOT_FOUND));
        String name = request.getName();
        if (name != null && name.trim().isEmpty()) throw invalidCategory();
        String description = category.getDescription();
        if (request.getDescription() != null) {
            JsonNode descriptionNode = request.getDescription();
            if (descriptionNode.isNull()) {
                description = null;
            } else if (descriptionNode.isTextual()) {
                description = descriptionNode.textValue();
            } else {
                throw invalidCategory();
            }
        }
        category.update(name == null ? null : name.trim(), description, request.getActive());
        try {
            return AdminCategory.from(categoryRepository.save(category));
        } catch (DataIntegrityViolationException exception) {
            throw categoryConflict();
        }
    }

    private static AuthException invalidCategory() {
        return new AuthException(
                "INVALID_CATEGORY", "Thông tin danh mục không hợp lệ.", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private static AuthException categoryConflict() {
        return new AuthException(
                "CATEGORY_SLUG_CONFLICT", "Slug danh mục đã được sử dụng.", HttpStatus.CONFLICT);
    }
}

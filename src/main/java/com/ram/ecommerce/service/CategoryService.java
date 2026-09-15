package com.ram.ecommerce.service;

import com.ram.ecommerce.model.Category;
import com.ram.ecommerce.repository.CategoryRepository;
import com.ram.ecommerce.view.CategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MinioService minioService;

    public CategoryService(CategoryRepository categoryRepository, MinioService minioService) {
        this.categoryRepository = categoryRepository;
        this.minioService = minioService;
    }

    public CategoryResponse create(String name, MultipartFile image) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Category already exists");
        }

        String imageUrl = minioService.uploadFile(image);

        Category category = new Category();
        category.setName(name);
        category.setImageUrl(imageUrl);
        categoryRepository.save(category);

        return toResponse(category);
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getImageUrl());
    }
}
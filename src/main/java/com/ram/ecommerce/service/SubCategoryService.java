package com.ram.ecommerce.service;

import com.ram.ecommerce.model.Category;
import com.ram.ecommerce.model.SubCategory;
import com.ram.ecommerce.repository.CategoryRepository;
import com.ram.ecommerce.repository.SubCategoryRepository;
import com.ram.ecommerce.view.SubCategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final MinioService minioService;

    public SubCategoryService(
            SubCategoryRepository subCategoryRepository,
            CategoryRepository categoryRepository,
            MinioService minioService
    ) {
        this.subCategoryRepository = subCategoryRepository;
        this.categoryRepository = categoryRepository;
        this.minioService = minioService;
    }

    public SubCategoryResponse create(Long categoryId, String name, MultipartFile image) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        String imageUrl = minioService.uploadFile(image);

        SubCategory subCategory = new SubCategory();
        subCategory.setCategory(category);
        subCategory.setName(name);
        subCategory.setImageUrl(imageUrl);
        subCategoryRepository.save(subCategory);

        return toResponse(subCategory);
    }

    public List<SubCategoryResponse> getByCategory(Long categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId).stream().map(this::toResponse).toList();
    }

    private SubCategoryResponse toResponse(SubCategory subCategory) {
        return new SubCategoryResponse(
                subCategory.getId(),
                subCategory.getName(),
                subCategory.getImageUrl(),
                subCategory.getCategory().getId()
        );
    }
}

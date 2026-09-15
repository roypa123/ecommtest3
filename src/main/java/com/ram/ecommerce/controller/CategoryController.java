package com.ram.ecommerce.controller;

import com.ram.ecommerce.service.CategoryService;
import com.ram.ecommerce.view.CategoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<CategoryResponse> create(
            @RequestParam("name") String name,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(categoryService.create(name, image));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }
}
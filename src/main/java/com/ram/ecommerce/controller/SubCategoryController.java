
package com.ram.ecommerce.controller;

import com.ram.ecommerce.service.SubCategoryService;
import com.ram.ecommerce.view.SubCategoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/categories/{categoryId}/subcategories")
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    public SubCategoryController(SubCategoryService subCategoryService) {
        this.subCategoryService = subCategoryService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<SubCategoryResponse> create(
            @PathVariable Long categoryId,
            @RequestParam("name") String name,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(subCategoryService.create(categoryId, name, image));
    }

    @GetMapping
    public ResponseEntity<List<SubCategoryResponse>> getAll(@PathVariable Long categoryId) {
        return ResponseEntity.ok(subCategoryService.getByCategory(categoryId));
    }
}
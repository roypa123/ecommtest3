package com.ram.ecommerce.controller;

import com.ram.ecommerce.service.ProductService;
import com.ram.ecommerce.view.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/subcategories/{subCategoryId}/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ProductResponse> create(
            @PathVariable Long subCategoryId,
            @RequestParam("name") String name,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(productService.create(subCategoryId, name, title, description, price, image));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll(@PathVariable Long subCategoryId) {
        return ResponseEntity.ok(productService.getBySubCategory(subCategoryId));
    }
}
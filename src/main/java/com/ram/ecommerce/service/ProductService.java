package com.ram.ecommerce.service;

import com.ram.ecommerce.model.Product;
import com.ram.ecommerce.model.SubCategory;
import com.ram.ecommerce.repository.ProductRepository;
import com.ram.ecommerce.repository.SubCategoryRepository;
import com.ram.ecommerce.view.ProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final MinioService minioService;

    public ProductService(
            ProductRepository productRepository,
            SubCategoryRepository subCategoryRepository,
            MinioService minioService
    ) {
        this.productRepository = productRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.minioService = minioService;
    }

    public ProductResponse create(
            Long subCategoryId,
            String name,
            String title,
            String description,
            BigDecimal price,
            MultipartFile image
    ) {
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Subcategory not found"));

        String imageUrl = minioService.uploadFile(image);

        Product product = new Product();
        product.setSubCategory(subCategory);
        product.setName(name);
        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        productRepository.save(product);

        return toResponse(product);
    }

    public List<ProductResponse> getBySubCategory(Long subCategoryId) {
        return productRepository.findBySubCategoryId(subCategoryId).stream().map(this::toResponse).toList();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getTitle(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice(),
                product.getSubCategory().getId()
        );
    }
}
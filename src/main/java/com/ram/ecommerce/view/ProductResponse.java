package com.ram.ecommerce.view;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String title,
        String description,
        String imageUrl,
        BigDecimal price,
        Long subCategoryId
) {}

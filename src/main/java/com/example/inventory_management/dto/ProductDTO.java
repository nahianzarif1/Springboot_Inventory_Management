package com.example.inventory_management.dto;

import java.math.BigDecimal;

public record ProductDTO(
        Long id,
        String sku,
        String name,
        BigDecimal price,
        int stockQuantity,
        Long categoryId,
        String categoryName,
        Long sellerId,
        String sellerUsername
) {
}

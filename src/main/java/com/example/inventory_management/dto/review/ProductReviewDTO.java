package com.example.inventory_management.dto.review;

import java.time.Instant;

public record ProductReviewDTO(
        Long id,
        Long productId,
        Long buyerId,
        String buyerUsername,
        int rating,
        String comment,
        Instant createdAt
) {
}


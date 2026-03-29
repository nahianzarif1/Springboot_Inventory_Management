package com.example.inventory_management.dto;

import java.time.Instant;

public record InventoryLogDTO(
        Long id,
        Long productId,
        String productName,
        int oldQuantity,
        int newQuantity,
        String changedByUsername,
        Instant createdAt
) {
}

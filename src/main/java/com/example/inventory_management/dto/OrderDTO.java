package com.example.inventory_management.dto;

import com.example.inventory_management.entity.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderDTO(
        Long id,
        Long buyerId,
        String buyerUsername,
        OrderStatus status,
        Instant createdAt,
        List<OrderItemDTO> items
) {
    public record OrderItemDTO(Long productId, String productName, int quantity) {}
}

package com.example.inventory_management.dto.cart;

import java.math.BigDecimal;

public record CartItemDTO(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity
) {
}

package com.example.inventory_management.dto;

import com.example.inventory_management.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDTO(
        Long id,
        Long buyerId,
        String buyerUsername,
        OrderStatus status,
        Instant createdAt,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        String couponCode,
        BigDecimal totalPrice,
        List<OrderItemDTO> items
) {
    public record OrderItemDTO(Long productId, String productName, int quantity, BigDecimal unitPrice) {
        public BigDecimal lineTotal() {
            BigDecimal up = unitPrice != null ? unitPrice : BigDecimal.ZERO;
            return up.multiply(BigDecimal.valueOf(quantity));
        }
    }
}

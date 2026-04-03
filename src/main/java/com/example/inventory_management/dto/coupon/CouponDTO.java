package com.example.inventory_management.dto.coupon;

import com.example.inventory_management.entity.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponDTO(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        Instant expiresAt,
        Integer usageLimit,
        int usageCount,
        boolean active,
        Instant createdAt
) {
}

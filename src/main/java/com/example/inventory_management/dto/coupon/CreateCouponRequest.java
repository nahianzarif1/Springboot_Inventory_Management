package com.example.inventory_management.dto.coupon;

import com.example.inventory_management.entity.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCouponRequest(
        @NotBlank @Size(min = 3, max = 40) String code,
        @NotNull DiscountType discountType,
        @NotNull @Positive BigDecimal discountValue,
        @NotNull Instant expiresAt,
        /** null = unlimited */
        @Positive Integer usageLimit
) {
}

package com.example.inventory_management.service;

import com.example.inventory_management.dto.coupon.CouponDTO;
import com.example.inventory_management.dto.coupon.CreateCouponRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CouponService {

    CouponDTO create(String sellerUsername, CreateCouponRequest request);

    List<CouponDTO> listForSeller(String sellerUsername);

    void deactivate(long couponId, String sellerUsername);

    /**
     * Computes discount for cart/order line items. Discount applies only to products sold by the coupon's seller.
     */
    DiscountCalculation calculateDiscount(String couponCodeOrBlank, List<OrderService.CreateItem> items);

    /**
     * Combines checkout coupon, per-seller codes, and per-product codes (productId → code).
     * For each cart line: product code wins; else seller-level code (checkout fills missing sellers; per-seller overrides).
     */
    CombinedDiscount calculateCombinedDiscount(
            String globalCouponCodeOrBlank,
            Map<Long, String> sellerIdToCouponCode,
            Map<Long, String> productIdToCouponCode,
            List<OrderService.CreateItem> items);

    record DiscountCalculation(BigDecimal discountAmount, com.example.inventory_management.entity.Coupon couponOrNull) {
        public static DiscountCalculation none() {
            return new DiscountCalculation(BigDecimal.ZERO, null);
        }
    }

    record CombinedDiscount(
            BigDecimal totalDiscount,
            java.util.List<com.example.inventory_management.entity.Coupon> couponsToIncrement,
            String appliedCodesSummary,
            com.example.inventory_management.entity.Coupon singleCouponOrNull
    ) {
        public static CombinedDiscount none() {
            return new CombinedDiscount(BigDecimal.ZERO, java.util.List.of(), null, null);
        }
    }
}

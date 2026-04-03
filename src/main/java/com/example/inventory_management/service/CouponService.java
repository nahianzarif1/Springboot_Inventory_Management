package com.example.inventory_management.service;

import com.example.inventory_management.dto.coupon.CouponDTO;
import com.example.inventory_management.dto.coupon.CreateCouponRequest;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    CouponDTO create(String sellerUsername, CreateCouponRequest request);

    List<CouponDTO> listForSeller(String sellerUsername);

    void deactivate(long couponId, String sellerUsername);

    /**
     * Computes discount for cart/order line items. Discount applies only to products sold by the coupon's seller.
     */
    DiscountCalculation calculateDiscount(String couponCodeOrBlank, List<OrderService.CreateItem> items);

    record DiscountCalculation(BigDecimal discountAmount, com.example.inventory_management.entity.Coupon couponOrNull) {
        public static DiscountCalculation none() {
            return new DiscountCalculation(BigDecimal.ZERO, null);
        }
    }
}

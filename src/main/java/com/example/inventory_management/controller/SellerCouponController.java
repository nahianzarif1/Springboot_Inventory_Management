package com.example.inventory_management.controller;

import com.example.inventory_management.dto.coupon.CouponDTO;
import com.example.inventory_management.dto.coupon.CreateCouponRequest;
import com.example.inventory_management.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seller/coupons")
public class SellerCouponController {

    private final CouponService couponService;

    public SellerCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public List<CouponDTO> list(Authentication authentication) {
        return couponService.listForSeller(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public CouponDTO create(@Valid @RequestBody CreateCouponRequest request, Authentication authentication) {
        return couponService.create(authentication.getName(), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public void deactivate(@PathVariable long id, Authentication authentication) {
        couponService.deactivate(id, authentication.getName());
    }
}

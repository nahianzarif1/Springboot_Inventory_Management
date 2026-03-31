package com.example.inventory_management.controller;

import com.example.inventory_management.dto.review.CreateProductReviewRequest;
import com.example.inventory_management.dto.review.ProductReviewDTO;
import com.example.inventory_management.service.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products/{productId}/reviews")
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    public ProductReviewController(ProductReviewService productReviewService) {
        this.productReviewService = productReviewService;
    }

    @GetMapping
    public List<ProductReviewDTO> list(@PathVariable long productId) {
        return productReviewService.listForProduct(productId);
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ProductReviewDTO createOrUpdate(@PathVariable long productId,
                                          @Valid @RequestBody CreateProductReviewRequest request,
                                          Authentication authentication) {
        return productReviewService.createOrUpdate(productId, authentication.getName(), request);
    }

    @DeleteMapping("/{reviewId}")
    public void delete(@PathVariable long productId,
                       @PathVariable long reviewId,
                       Authentication authentication) {
        productReviewService.delete(productId, reviewId, authentication.getName(), isAdmin(authentication));
    }

    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}


package com.example.inventory_management.service;

import com.example.inventory_management.dto.review.CreateProductReviewRequest;
import com.example.inventory_management.dto.review.ProductReviewDTO;

import java.util.List;

public interface ProductReviewService {
    ProductReviewDTO createOrUpdate(long productId, String buyerUsername, CreateProductReviewRequest request);

    List<ProductReviewDTO> listForProduct(long productId);

    void delete(long productId, long reviewId, String username, boolean admin);
}


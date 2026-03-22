package com.example.inventory_management.service;

import com.example.inventory_management.dto.ProductDTO;
import com.example.inventory_management.dto.product.ProductCreateRequest;
import com.example.inventory_management.dto.product.ProductUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductDTO createProduct(ProductCreateRequest request, String sellerUsername);
    ProductDTO updateProduct(long productId, ProductUpdateRequest request, String sellerUsername);
    void deleteProduct(long productId, String sellerUsername);
    ProductDTO findProductById(long productId);
    Page<ProductDTO> searchProducts(String q, Pageable pageable);
}

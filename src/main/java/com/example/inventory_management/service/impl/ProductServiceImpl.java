package com.example.inventory_management.service.impl;

import com.example.inventory_management.dto.ProductDTO;
import com.example.inventory_management.dto.product.ProductCreateRequest;
import com.example.inventory_management.dto.product.ProductUpdateRequest;
import com.example.inventory_management.entity.Category;
import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.CategoryRepository;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ProductDTO createProduct(ProductCreateRequest request, String sellerUsername) {
        if (request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Product price cannot be negative");
        }
        if (request.stockQuantity() < 0) {
            throw new ConflictException("Product quantity cannot be negative");
        }
        if (productRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new ConflictException("Duplicate SKU");
        }

        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Product p = Product.builder()
                .sku(request.sku())
                .name(request.name())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .category(category)
                .seller(seller)
                .build();

        try {
            p = productRepository.save(p);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Duplicate product");
        }

        return toDto(p);
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(long productId, ProductUpdateRequest request, String sellerUsername) {
        if (request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Product price cannot be negative");
        }
        if (request.stockQuantity() < 0) {
            throw new ConflictException("Product quantity cannot be negative");
        }

        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        Product p = productRepository.findByIdAndSeller(productId, seller)
                .orElseThrow(() -> new ConflictException("Seller cannot edit another seller's product"));

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        p.setName(request.name());
        p.setPrice(request.price());
        p.setStockQuantity(request.stockQuantity());
        p.setCategory(category);

        return toDto(p);
    }

    @Override
    @Transactional
    public void deleteProduct(long productId, String sellerUsername) {
        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        Product p = productRepository.findByIdAndSeller(productId, seller)
                .orElseThrow(() -> new ConflictException("Seller cannot delete another seller's product"));
        productRepository.delete(p);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO findProductById(long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toDto(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return productRepository.findAll(pageable).map(this::toDto);
        }
        return productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(q, q, pageable)
                .map(this::toDto);
    }

    private ProductDTO toDto(Product p) {
        Long categoryId = p.getCategory() != null ? p.getCategory().getId() : null;
        String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;
        Long sellerId = p.getSeller() != null ? p.getSeller().getId() : null;
        String sellerUsername = p.getSeller() != null ? p.getSeller().getUsername() : null;

        return new ProductDTO(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getPrice(),
                p.getStockQuantity(),
                categoryId,
                categoryName,
                sellerId,
                sellerUsername
        );
    }
}

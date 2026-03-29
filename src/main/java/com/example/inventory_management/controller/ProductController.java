package com.example.inventory_management.controller;

import com.example.inventory_management.dto.ProductDTO;
import com.example.inventory_management.dto.product.ProductCreateRequest;
import com.example.inventory_management.dto.product.ProductUpdateRequest;
import com.example.inventory_management.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Page<ProductDTO> list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.searchProducts(q, categoryId, pageable);
    }

    @GetMapping("/{id}")
    public ProductDTO get(@PathVariable long id) {
        return productService.findProductById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ProductDTO create(@Valid @RequestBody ProductCreateRequest request, Authentication authentication) {
        return productService.createProduct(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ProductDTO update(@PathVariable long id, @Valid @RequestBody ProductUpdateRequest request, Authentication authentication) {
        return productService.updateProduct(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public void delete(@PathVariable long id, Authentication authentication) {
        productService.deleteProduct(id, authentication.getName());
    }
}

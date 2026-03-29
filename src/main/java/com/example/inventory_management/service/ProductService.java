package com.example.inventory_management.service;

import com.example.inventory_management.dto.InventoryLogDTO;
import com.example.inventory_management.dto.ProductDTO;
import com.example.inventory_management.dto.product.ProductCreateRequest;
import com.example.inventory_management.dto.product.ProductUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    ProductDTO createProduct(ProductCreateRequest request, String sellerUsername);

    ProductDTO updateProduct(long productId, ProductUpdateRequest request, String sellerUsername);

    void deleteProduct(long productId, String sellerUsername);

    ProductDTO findProductById(long productId);

    Page<ProductDTO> searchProducts(String q, Long categoryId, Pageable pageable);

    List<ProductDTO> listAllProducts();

    void deleteProductByAdmin(long productId);

    void adjustStockByAdmin(long productId, int newQuantity, String adminUsername);

    List<ProductDTO> lowStockForSeller(String sellerUsername, int threshold);

    List<InventoryLogDTO> inventoryHistoryForSeller(String sellerUsername);

    void saveProductImage(long productId, MultipartFile file, String username);
}

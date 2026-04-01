package com.example.inventory_management.repository;

import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    Optional<Product> findByIdAndSeller(Long id, User seller);

    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(String name, String sku, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE (:q IS NULL OR :q = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            """)
    Page<Product> search(@Param("q") String q, @Param("categoryId") Long categoryId, Pageable pageable);

    List<Product> findBySellerAndStockQuantityLessThanEqual(User seller, int maxStock);

    boolean existsBySeller_IdAndNameIgnoreCase(Long sellerId, String name);

    boolean existsBySeller_IdAndNameIgnoreCaseAndIdNot(Long sellerId, String name, Long id);

    @Query("""
            SELECT p FROM Product p
            WHERE p.stockQuantity <= :maxStock
            ORDER BY p.stockQuantity ASC, p.id DESC
            """)
    List<Product> findLowStockGlobal(@Param("maxStock") int maxStock, Pageable pageable);
}

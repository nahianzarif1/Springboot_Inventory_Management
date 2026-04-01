package com.example.inventory_management.repository;

import com.example.inventory_management.entity.InventoryLog;
import com.example.inventory_management.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findByProductOrderByCreatedAtDesc(Product product);

    @Query("SELECT l FROM InventoryLog l WHERE l.product.seller.id = :sellerId ORDER BY l.createdAt DESC")
    List<InventoryLog> findBySellerId(@Param("sellerId") Long sellerId);

    void deleteByProduct(Product product);
}

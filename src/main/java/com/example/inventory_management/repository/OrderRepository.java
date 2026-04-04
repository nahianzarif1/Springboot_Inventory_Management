package com.example.inventory_management.repository;

import com.example.inventory_management.entity.Order;
import com.example.inventory_management.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Loads buyer, line items, products, and optional coupon in one round trip.
     * Uses {@link EntityGraph} instead of DISTINCT FETCH JOIN (fewer PostgreSQL/SQL edge cases).
     */
    @EntityGraph(attributePaths = {"buyer", "coupon", "items", "items.product"})
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"buyer", "coupon", "items", "items.product"})
    List<Order> findByBuyerOrderByCreatedAtDesc(User buyer);

    @EntityGraph(attributePaths = {"buyer", "coupon", "items", "items.product"})
    @Query("""
            SELECT DISTINCT o FROM Order o
            JOIN o.items i
            WHERE i.product.seller.id = :sellerId
            ORDER BY o.createdAt DESC
            """)
    List<Order> findDistinctBySellerProducts(@Param("sellerId") Long sellerId);
}

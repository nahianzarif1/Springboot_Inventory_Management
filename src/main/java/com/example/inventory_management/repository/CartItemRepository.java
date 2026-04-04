package com.example.inventory_management.repository;

import com.example.inventory_management.entity.CartItem;
import com.example.inventory_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByBuyer(User buyer);

    /** Loads products (and sellers) in one query — avoids lazy-load failures during checkout. */
    @Query("""
            SELECT ci FROM CartItem ci
            JOIN FETCH ci.product p
            LEFT JOIN FETCH p.seller
            WHERE ci.buyer = :buyer
            """)
    List<CartItem> findByBuyerWithProductDetails(@Param("buyer") User buyer);
    Optional<CartItem> findByBuyerIdAndProductId(Long buyerId, Long productId);
    void deleteByBuyer(User buyer);
    void deleteByProductId(Long productId);
}

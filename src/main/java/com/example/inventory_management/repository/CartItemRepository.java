package com.example.inventory_management.repository;

import com.example.inventory_management.entity.CartItem;
import com.example.inventory_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByBuyer(User buyer);
    Optional<CartItem> findByBuyerIdAndProductId(Long buyerId, Long productId);
    void deleteByBuyer(User buyer);
}

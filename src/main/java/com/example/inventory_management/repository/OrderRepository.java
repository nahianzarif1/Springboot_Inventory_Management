package com.example.inventory_management.repository;

import com.example.inventory_management.entity.Order;
import com.example.inventory_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyer(User buyer);
}

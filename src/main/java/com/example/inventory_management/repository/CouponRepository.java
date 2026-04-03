package com.example.inventory_management.repository;

import com.example.inventory_management.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    List<Coupon> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}

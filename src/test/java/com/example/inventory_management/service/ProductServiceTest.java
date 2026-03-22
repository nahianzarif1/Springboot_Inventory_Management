package com.example.inventory_management.service;

import com.example.inventory_management.dto.product.ProductCreateRequest;
import com.example.inventory_management.dto.product.ProductUpdateRequest;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.inventory_management.service.impl.ProductServiceImpl;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(ProductServiceImpl.class)
class ProductServiceTest {

    @Autowired ProductService productService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    @BeforeEach
    void setup() {
        userRepository.save(User.builder()
                .username("seller1")
                .passwordHash("x")
                .roles(Set.of(Role.SELLER))
                .enabled(true)
                .build());
    }

    @Test
    void createProduct_rejectsDuplicateSku() {
        productService.createProduct(new ProductCreateRequest("SKU1", "P1", BigDecimal.TEN, 1, null), "seller1");
        assertThrows(ConflictException.class,
                () -> productService.createProduct(new ProductCreateRequest("SKU1", "P2", BigDecimal.ONE, 1, null), "seller1"));
    }

    @Test
    void createProduct_rejectsNegativePrice() {
        assertThrows(ConflictException.class,
                () -> productService.createProduct(new ProductCreateRequest("SKU2", "P", new BigDecimal("-1"), 1, null), "seller1"));
    }

    @Test
    void updateProduct_rejectsOtherSeller() {
        var p = productService.createProduct(new ProductCreateRequest("SKU3", "P3", BigDecimal.ONE, 1, null), "seller1");

        userRepository.save(User.builder()
                .username("seller2")
                .passwordHash("x")
                .roles(Set.of(Role.SELLER))
                .enabled(true)
                .build());

        assertThrows(ConflictException.class,
                () -> productService.updateProduct(p.id(), new ProductUpdateRequest("X", BigDecimal.ONE, 1, null), "seller2"));
    }
}

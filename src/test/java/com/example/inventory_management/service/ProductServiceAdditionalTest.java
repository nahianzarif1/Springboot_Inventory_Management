package com.example.inventory_management.service;

import com.example.inventory_management.dto.product.ProductCreateRequest;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(ProductServiceImpl.class)
class ProductServiceAdditionalTest {

    @Autowired ProductService productService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    @BeforeEach
    void setup() {
        userRepository.save(User.builder().username("sellerX").passwordHash("x").roles(Set.of(Role.SELLER)).enabled(true).build());
    }

    @Test
    void createProduct_rejectsNegativeStock() {
        assertThrows(ConflictException.class,
                () -> productService.createProduct(new ProductCreateRequest("SKU10", "P", BigDecimal.ONE, -1, null), "sellerX"));
    }

    @Test
    void deleteProduct_otherSeller_throws() {
        var p = productService.createProduct(new ProductCreateRequest("SKU11", "P", BigDecimal.ONE, 1, null), "sellerX");
        userRepository.save(User.builder().username("sellerY").passwordHash("x").roles(Set.of(Role.SELLER)).enabled(true).build());
        assertThrows(ConflictException.class, () -> productService.deleteProduct(p.id(), "sellerY"));
    }
}

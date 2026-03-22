package com.example.inventory_management.service;

import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.InsufficientStockException;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.inventory_management.service.impl.CartServiceImpl;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(CartServiceImpl.class)
class CartServiceTest {

    @Autowired CartService cartService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    private Long productId;

    @BeforeEach
    void setup() {
        userRepository.save(User.builder().username("buyer").passwordHash("x").roles(Set.of(Role.BUYER)).enabled(true).build());
        userRepository.save(User.builder().username("seller").passwordHash("x").roles(Set.of(Role.SELLER)).enabled(true).build());
        var seller = userRepository.findByUsernameIgnoreCase("seller").orElseThrow();
        Product p = Product.builder().sku("S1").name("N").price(BigDecimal.ONE).stockQuantity(2).seller(seller).build();
        productId = productRepository.save(p).getId();
    }

    @Test
    void addSameProductTwice_increments() {
        cartService.addToCart("buyer", productId, 1);
        cartService.addToCart("buyer", productId, 1);
        assertEquals(1, cartService.getCart("buyer").size());
        assertEquals(2, cartService.getCart("buyer").get(0).getQuantity());
    }

    @Test
    void addMoreThanStock_throws() {
        cartService.addToCart("buyer", productId, 2);
        assertThrows(InsufficientStockException.class, () -> cartService.addToCart("buyer", productId, 1));
    }
}

package com.example.inventory_management.service;

import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(CartServiceImpl.class)
class CartServiceAdditionalTest {

    @Autowired CartService cartService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    private Long productId;

    @BeforeEach
    void setup() {
        userRepository.save(User.builder().username("buyerA").passwordHash("x").roles(Set.of(Role.BUYER)).enabled(true).build());
        userRepository.save(User.builder().username("sellerA").passwordHash("x").roles(Set.of(Role.SELLER)).enabled(true).build());
        var seller = userRepository.findByUsernameIgnoreCase("sellerA").orElseThrow();
        Product p = Product.builder().sku("S2").name("N2").price(BigDecimal.ONE).stockQuantity(1).seller(seller).build();
        productId = productRepository.save(p).getId();
    }

    @Test
    void removeMissingCartItem_throws() {
        assertThrows(ResourceNotFoundException.class, () -> cartService.removeFromCart("buyerA", productId));
    }

    @Test
    void getCart_returnsEmptyWhenNone() {
        assertTrue(cartService.getCart("buyerA").isEmpty());
    }
}

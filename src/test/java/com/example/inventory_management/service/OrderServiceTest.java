package com.example.inventory_management.service;

import com.example.inventory_management.entity.*;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.InsufficientStockException;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.inventory_management.repository.CartItemRepository;
import com.example.inventory_management.repository.OrderRepository;
import com.example.inventory_management.service.impl.OrderServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(OrderServiceImpl.class)
class OrderServiceTest {

    @Autowired OrderService orderService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired OrderRepository orderRepository;

    private Long productId;

    @BeforeEach
    void setup() {
        var buyer = userRepository.save(User.builder().username("buyer").passwordHash("x").roles(Set.of(Role.BUYER)).enabled(true).build());
        var seller = userRepository.save(User.builder().username("seller").passwordHash("x").roles(Set.of(Role.SELLER)).enabled(true).build());
        productId = productRepository.save(Product.builder().sku("SKU").name("P").price(BigDecimal.ONE).stockQuantity(1).seller(seller).build()).getId();
        cartItemRepository.save(CartItem.builder().buyer(buyer).product(productRepository.findById(productId).orElseThrow()).quantity(1).build());
    }

    @Test
    void createOrderFromCart_decrementsStock() {
        orderService.createOrderFromCart("buyer");
        assertEquals(0, productRepository.findById(productId).orElseThrow().getStockQuantity());
        assertTrue(cartItemRepository.findByBuyer(userRepository.findByUsernameIgnoreCase("buyer").orElseThrow()).isEmpty());
    }

    @Test
    void createOrder_moreThanStock_throws() {
        assertThrows(InsufficientStockException.class,
                () -> orderService.createOrder("buyer", List.of(new OrderService.CreateItem(productId, 2))));
    }

    @Test
    void cancelAfterShipped_throws() {
        var dto = orderService.createOrderFromCart("buyer");
        orderService.updateStatus(dto.id(), OrderStatus.SHIPPED);
        assertThrows(ConflictException.class, () -> orderService.updateStatus(dto.id(), OrderStatus.CANCELED));
    }
}

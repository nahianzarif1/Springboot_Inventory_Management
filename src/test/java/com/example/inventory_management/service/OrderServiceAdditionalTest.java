package com.example.inventory_management.service;

import com.example.inventory_management.entity.*;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(OrderServiceImpl.class)
class OrderServiceAdditionalTest {

    @Autowired OrderService orderService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    private Long productId;

    @BeforeEach
    void setup() {
        var buyer1 = userRepository.save(User.builder().username("buyer1").passwordHash("x").roles(Set.of(Role.BUYER)).enabled(true).build());
        var buyer2 = userRepository.save(User.builder().username("buyer2").passwordHash("x").roles(Set.of(Role.BUYER)).enabled(true).build());
        var seller = userRepository.save(User.builder().username("seller1").passwordHash("x").roles(Set.of(Role.SELLER)).enabled(true).build());
        productId = productRepository.save(Product.builder().sku("SKU20").name("P").price(BigDecimal.ONE).stockQuantity(2).seller(seller).build()).getId();
        orderService.createOrder(buyer1.getUsername(), List.of(new OrderService.CreateItem(productId, 1)));
    }

    @Test
    void buyerCannotAccessOtherBuyerOrder() {
        var order = orderService.listOrders("buyer1", false).get(0);
        assertThrows(ConflictException.class, () -> orderService.getOrder(order.id(), "buyer2", false));
    }
}

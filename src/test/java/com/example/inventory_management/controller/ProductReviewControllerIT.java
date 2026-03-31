package com.example.inventory_management.controller;

import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.OrderStatus;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductReviewControllerIT {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired OrderService orderService;

    private long productId;

    @BeforeEach
    void setup() {
        User seller = userRepository.findByUsernameIgnoreCase("sellerReviewIT").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("sellerReviewIT")
                        .email("sellerReviewIT@test.com")
                        .passwordHash(encoder.encode("pw"))
                        .roles(Set.of(Role.SELLER))
                        .enabled(true)
                        .build()));

        userRepository.findByUsernameIgnoreCase("buyerReviewIT").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("buyerReviewIT")
                        .email("buyerReviewIT@test.com")
                        .passwordHash(encoder.encode("pw"))
                        .roles(Set.of(Role.BUYER))
                        .enabled(true)
                        .build()));

        Product p = productRepository.save(Product.builder()
                .sku("SKU-REVIEW-IT")
                .name("Review product")
                .description("Desc")
                .price(new BigDecimal("100.00"))
                .stockQuantity(50)
                .seller(seller)
                .build());
        productId = p.getId();

        // Buyer purchases and pays so they become eligible to review
        var order = orderService.createOrder("buyerReviewIT", List.of(new OrderService.CreateItem(productId, 1)));
        orderService.updateStatus(order.id(), OrderStatus.PAID);
    }

    @Test
    void buyerCanCreateAndListReviewsAfterPaidPurchase() throws Exception {
        mvc.perform(post("/products/" + productId + "/reviews")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("buyerReviewIT", "pw"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"comment":"Great product"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));

        mvc.perform(get("/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].buyerUsername").value("buyerReviewIT"))
                .andExpect(jsonPath("$[0].rating").value(5));
    }
}


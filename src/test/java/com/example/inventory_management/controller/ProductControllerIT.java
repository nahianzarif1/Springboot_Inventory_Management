package com.example.inventory_management.controller;

import com.example.inventory_management.dto.product.ProductCreateRequest;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIT {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.findByUsernameIgnoreCase("sellerIT").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("sellerIT")
                        .passwordHash(passwordEncoder.encode("pw"))
                        .roles(Set.of(Role.SELLER))
                        .enabled(true)
                        .build()));
    }

    @Test
    void sellerCanCreateProduct() throws Exception {
        String json = "{\"sku\":\"IT1\",\"name\":\"P\",\"price\":10,\"stockQuantity\":1}";

        mvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("sellerIT", "pw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("IT1"));
    }

    @Test
    void unauthCannotCreateProduct() throws Exception {
        String json = "{\"sku\":\"IT2\",\"name\":\"P\",\"price\":10,\"stockQuantity\":1}";
        mvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }
}

package com.example.inventory_management.controller;

import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessIT {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setup() {
        userRepository.findByUsernameIgnoreCase("adminIT").orElseGet(() -> userRepository.save(User.builder()
                .username("adminIT").passwordHash(encoder.encode("pw")).roles(Set.of(Role.ADMIN)).enabled(true).build()));
        userRepository.findByUsernameIgnoreCase("buyerIT").orElseGet(() -> userRepository.save(User.builder()
                .username("buyerIT").passwordHash(encoder.encode("pw")).roles(Set.of(Role.BUYER)).enabled(true).build()));
    }

    @Test
    void buyerCannotAccessAdminEndpoints() throws Exception {
        mvc.perform(get("/admin/users")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("buyerIT", "pw")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminEndpoints() throws Exception {
        mvc.perform(get("/admin/users")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("adminIT", "pw")))
                .andExpect(status().isOk());
    }
}

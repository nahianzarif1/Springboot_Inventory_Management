package com.example.inventory_management.service;

import com.example.inventory_management.config.PasswordConfig;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({AuthServiceImpl.class, PasswordConfig.class})
class AuthServiceTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    @Test
    void registerBuyer_createsUserWithBuyerRole() {
        var dto = authService.registerBuyer("newbuyer", "secret123");
        User u = userRepository.findByUsernameIgnoreCase("newbuyer").orElseThrow();
        assertEquals(dto.id(), u.getId());
        assertTrue(u.getRoles().contains(Role.BUYER));
    }

    @Test
    void registerBuyer_duplicateUsername_throws() {
        userRepository.save(User.builder()
                .username("dup")
                .passwordHash("x")
                .roles(Set.of(Role.BUYER))
                .enabled(true)
                .build());
        assertThrows(ConflictException.class, () -> authService.registerBuyer("dup", "secret123"));
    }
}

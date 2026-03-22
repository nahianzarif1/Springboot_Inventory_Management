package com.example.inventory_management.service;

import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(UserServiceImpl.class)
class UserServiceTest {

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;

    @Test
    void updateUserRole_changesRole() {
        var user = userRepository.save(User.builder().username("u1").passwordHash("x").roles(Set.of(Role.BUYER)).enabled(true).build());
        var dto = userService.updateUserRole(user.getId(), Role.ADMIN);
        assertTrue(dto.roles().contains(Role.ADMIN));
    }

    @Test
    void deleteMissingUser_throws() {
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(999));
    }
}

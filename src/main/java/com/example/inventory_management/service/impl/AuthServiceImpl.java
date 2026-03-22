package com.example.inventory_management.service.impl;

import com.example.inventory_management.dto.UserDTO;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDTO registerBuyer(String username, String rawPassword) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already exists");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .roles(Set.of(Role.BUYER))
                .enabled(true)
                .build();

        user = userRepository.save(user);
        return new UserDTO(user.getId(), user.getUsername(), user.getRoles(), user.isEnabled());
    }
}

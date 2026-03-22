package com.example.inventory_management.config;

import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;

    @Bean
    ApplicationRunner seedUsers(PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsernameIgnoreCase("admin")) {
                userRepository.save(User.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .roles(Set.of(Role.ADMIN))
                        .enabled(true)
                        .build());
            }
            if (!userRepository.existsByUsernameIgnoreCase("seller")) {
                userRepository.save(User.builder()
                        .username("seller")
                        .passwordHash(passwordEncoder.encode("seller123"))
                        .roles(Set.of(Role.SELLER))
                        .enabled(true)
                        .build());
            }
            if (!userRepository.existsByUsernameIgnoreCase("buyer")) {
                userRepository.save(User.builder()
                        .username("buyer")
                        .passwordHash(passwordEncoder.encode("buyer123"))
                        .roles(Set.of(Role.BUYER))
                        .enabled(true)
                        .build());
            }
        };
    }
}

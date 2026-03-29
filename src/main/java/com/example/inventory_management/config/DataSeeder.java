package com.example.inventory_management.config;

import com.example.inventory_management.entity.Category;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.repository.CategoryRepository;
import com.example.inventory_management.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataSeeder {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Bean
    ApplicationRunner seedData(PasswordEncoder passwordEncoder) {
        return args -> {
            if (categoryRepository.count() == 0) {
                categoryRepository.save(Category.builder().name("Electronics").description("Devices and gadgets").build());
                categoryRepository.save(Category.builder().name("Books").description("All kinds of books").build());
                categoryRepository.save(Category.builder().name("Clothing").description("Wearables").build());
            }

            if (!userRepository.existsByUsernameIgnoreCase("admin")) {
                userRepository.save(User.builder()
                        .username("admin")
                        .email("admin@mail.com")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .roles(Set.of(Role.ADMIN))
                        .enabled(true)
                        .build());
            }
            if (!userRepository.existsByUsernameIgnoreCase("seller")) {
                userRepository.save(User.builder()
                        .username("seller")
                        .email("seller@mail.com")
                        .passwordHash(passwordEncoder.encode("seller123"))
                        .roles(Set.of(Role.SELLER))
                        .enabled(true)
                        .build());
            }
            if (!userRepository.existsByUsernameIgnoreCase("buyer")) {
                userRepository.save(User.builder()
                        .username("buyer")
                        .email("buyer@mail.com")
                        .passwordHash(passwordEncoder.encode("buyer123"))
                        .roles(Set.of(Role.BUYER))
                        .enabled(true)
                        .build());
            }
        };
    }
}

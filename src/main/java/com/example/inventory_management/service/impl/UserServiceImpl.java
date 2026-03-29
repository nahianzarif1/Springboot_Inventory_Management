package com.example.inventory_management.service.impl;

import com.example.inventory_management.dto.UserDTO;
import com.example.inventory_management.dto.user.AdminUserCreateRequest;
import com.example.inventory_management.dto.user.AdminUserUpdateRequest;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private UserDTO toDto(User u) {
        return new UserDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRoles(), u.isEnabled());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getByUsername(String username) {
        User u = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toDto(u);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> listUsers() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public UserDTO createUser(AdminUserCreateRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already exists");
        }
        User u = User.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(new HashSet<>(request.roles()))
                .enabled(true)
                .build();
        u = userRepository.save(u);
        return toDto(u);
    }

    @Override
    @Transactional
    public UserDTO updateUser(long userId, AdminUserUpdateRequest request) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean noUsername = request.username() == null || request.username().isBlank();
        boolean noEmail = request.email() == null || request.email().isBlank();
        if (noUsername && noEmail) {
            return toDto(u);
        }
        if (request.username() != null && !request.username().isBlank()) {
            String nu = request.username().trim();
            if (!u.getUsername().equalsIgnoreCase(nu) && userRepository.existsByUsernameIgnoreCase(nu)) {
                throw new ConflictException("Username already exists");
            }
            u.setUsername(nu);
        }
        if (request.email() != null && !request.email().isBlank()) {
            String ne = request.email().trim().toLowerCase();
            if (!u.getEmail().equalsIgnoreCase(ne) && userRepository.existsByEmailIgnoreCase(ne)) {
                throw new ConflictException("Email already exists");
            }
            u.setEmail(ne);
        }
        return toDto(u);
    }

    @Override
    @Transactional
    public UserDTO setEnabled(long userId, boolean enabled) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        u.setEnabled(enabled);
        return toDto(u);
    }

    @Override
    @Transactional
    public UserDTO updateUserRole(long userId, Role role) {
        return updateUserRoles(userId, Set.of(role));
    }

    @Override
    @Transactional
    public UserDTO updateUserRoles(long userId, Set<Role> roles) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (roles == null || roles.isEmpty()) {
            throw new ConflictException("At least one role is required");
        }
        u.setRoles(new HashSet<>(roles));
        return toDto(u);
    }

    @Override
    @Transactional
    public void deleteUser(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(userId);
    }
}

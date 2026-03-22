package com.example.inventory_management.service.impl;

import com.example.inventory_management.dto.UserDTO;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDTO getByUsername(String username) {
        User u = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserDTO(u.getId(), u.getUsername(), u.getRoles(), u.isEnabled());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDTO(u.getId(), u.getUsername(), u.getRoles(), u.isEnabled()))
                .toList();
    }

    @Override
    @Transactional
    public UserDTO updateUserRole(long userId, Role role) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Set<Role> newRoles = new java.util.HashSet<>();
        newRoles.add(role);
        u.setRoles(newRoles);
        return new UserDTO(u.getId(), u.getUsername(), u.getRoles(), u.isEnabled());
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

package com.example.inventory_management.dto;

import com.example.inventory_management.entity.Role;

import java.util.Set;

public record UserDTO(Long id, String username, Set<Role> roles, boolean enabled) {
}

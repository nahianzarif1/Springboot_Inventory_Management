package com.example.inventory_management.dto.user;

import com.example.inventory_management.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminUserCreateRequest(
        @NotBlank @Size(min = 3, max = 60) String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotEmpty Set<Role> roles
) {
}

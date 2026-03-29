package com.example.inventory_management.service;

import com.example.inventory_management.dto.UserDTO;
import com.example.inventory_management.dto.user.AdminUserCreateRequest;
import com.example.inventory_management.dto.user.AdminUserUpdateRequest;
import com.example.inventory_management.entity.Role;

import java.util.List;
import java.util.Set;

public interface UserService {
    UserDTO getByUsername(String username);

    List<UserDTO> listUsers();

    UserDTO createUser(AdminUserCreateRequest request);

    UserDTO updateUser(long userId, AdminUserUpdateRequest request);

    UserDTO setEnabled(long userId, boolean enabled);

    UserDTO updateUserRole(long userId, Role role);

    UserDTO updateUserRoles(long userId, Set<Role> roles);

    void deleteUser(long userId);
}

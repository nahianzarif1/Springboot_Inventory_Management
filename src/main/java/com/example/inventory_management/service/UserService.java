package com.example.inventory_management.service;

import com.example.inventory_management.dto.UserDTO;
import com.example.inventory_management.entity.Role;

import java.util.List;

public interface UserService {
    UserDTO getByUsername(String username);
    List<UserDTO> listUsers();
    UserDTO updateUserRole(long userId, Role role);
    void deleteUser(long userId);
}

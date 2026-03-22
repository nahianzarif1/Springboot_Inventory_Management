package com.example.inventory_management.service;

import com.example.inventory_management.dto.UserDTO;

public interface AuthService {
    UserDTO registerBuyer(String username, String rawPassword);
}

package com.example.inventory_management.controller;

import com.example.inventory_management.dto.UserDTO;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public List<UserDTO> listUsers() {
        return userService.listUsers();
    }

    public record UpdateRoleRequest(Role role) {}

    @PutMapping("/users/{id}/role")
    public UserDTO updateRole(@PathVariable long id, @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateUserRole(id, request.role());
    }

    @DeleteMapping("/users/{id}")
    public void delete(@PathVariable long id) {
        userService.deleteUser(id);
    }
}

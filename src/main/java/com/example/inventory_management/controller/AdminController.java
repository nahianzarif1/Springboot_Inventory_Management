package com.example.inventory_management.controller;

import com.example.inventory_management.dto.UserDTO;
import com.example.inventory_management.dto.user.AdminUserCreateRequest;
import com.example.inventory_management.dto.user.AdminUserUpdateRequest;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<UserDTO> listUsers() {
        return userService.listUsers();
    }

    @PostMapping("/users")
    public UserDTO createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return userService.createUser(request);
    }

    public record UpdateRoleRequest(Role role) {}

    @PutMapping("/users/{id}/role")
    public UserDTO updateRole(@PathVariable long id, @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateUserRole(id, request.role());
    }

    public record UpdateRolesRequest(Set<Role> roles) {}

    @PutMapping("/users/{id}/roles")
    public UserDTO updateRoles(@PathVariable long id, @Valid @RequestBody UpdateRolesRequest request) {
        return userService.updateUserRoles(id, request.roles());
    }

    @PutMapping("/users/{id}")
    public UserDTO updateUser(@PathVariable long id, @Valid @RequestBody AdminUserUpdateRequest request) {
        return userService.updateUser(id, request);
    }

    public record EnabledRequest(boolean enabled) {}

    @PatchMapping("/users/{id}/enabled")
    public UserDTO setEnabled(@PathVariable long id, @Valid @RequestBody EnabledRequest request) {
        return userService.setEnabled(id, request.enabled());
    }

    @DeleteMapping("/users/{id}")
    public void delete(@PathVariable long id) {
        userService.deleteUser(id);
    }
}

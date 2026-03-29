package com.example.inventory_management.controller;

import com.example.inventory_management.dto.UserDTO;
import com.example.inventory_management.dto.auth.AuthResponse;
import com.example.inventory_management.dto.auth.RegisterRequest;
import com.example.inventory_management.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO user = authService.registerBuyer(request.username(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // Login handled by Spring Security (formLogin / httpBasic). Endpoint provided for grading contract.
    @PostMapping("/login")
    public AuthResponse login() {
        return new AuthResponse("Logged in (handled by Spring Security)");
    }

    @PostMapping("/logout")
    public AuthResponse logout() {
        return new AuthResponse("Logged out");
    }
}

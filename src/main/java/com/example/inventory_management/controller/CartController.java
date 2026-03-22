package com.example.inventory_management.controller;

import com.example.inventory_management.dto.OrderDTO;
import com.example.inventory_management.dto.cart.AddToCartRequest;
import com.example.inventory_management.dto.cart.RemoveFromCartRequest;
import com.example.inventory_management.entity.CartItem;
import com.example.inventory_management.service.CartService;
import com.example.inventory_management.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    @PostMapping("/add")
    public void add(@Valid @RequestBody AddToCartRequest request, Authentication authentication) {
        cartService.addToCart(authentication.getName(), request.productId(), request.quantity());
    }

    @DeleteMapping("/remove")
    public void remove(@Valid @RequestBody RemoveFromCartRequest request, Authentication authentication) {
        cartService.removeFromCart(authentication.getName(), request.productId());
    }

    @GetMapping
    public List<CartItem> get(Authentication authentication) {
        return cartService.getCart(authentication.getName());
    }

    @PostMapping("/checkout")
    public OrderDTO checkout(Authentication authentication) {
        return orderService.createOrderFromCart(authentication.getName());
    }
}

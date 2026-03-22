package com.example.inventory_management.service;

import com.example.inventory_management.entity.CartItem;

import java.util.List;

public interface CartService {
    void addToCart(String buyerUsername, long productId, int quantity);
    void removeFromCart(String buyerUsername, long productId);
    List<CartItem> getCart(String buyerUsername);
    void clearCart(String buyerUsername);
}

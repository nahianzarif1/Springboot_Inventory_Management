package com.example.inventory_management.dto.cart;

import jakarta.validation.constraints.NotNull;

public record RemoveFromCartRequest(@NotNull Long productId) {
}

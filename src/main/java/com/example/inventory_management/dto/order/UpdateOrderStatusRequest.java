package com.example.inventory_management.dto.order;

import com.example.inventory_management.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}

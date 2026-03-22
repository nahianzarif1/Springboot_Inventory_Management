package com.example.inventory_management.controller;

import com.example.inventory_management.dto.OrderDTO;
import com.example.inventory_management.dto.order.CreateOrderRequest;
import com.example.inventory_management.dto.order.UpdateOrderStatusRequest;
import com.example.inventory_management.entity.OrderStatus;
import com.example.inventory_management.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderDTO create(@Valid @RequestBody(required = false) CreateOrderRequest request, Authentication authentication) {
        if (request == null) {
            return orderService.createOrderFromCart(authentication.getName());
        }
        return orderService.createOrder(authentication.getName(),
                request.items().stream().map(i -> new OrderService.CreateItem(i.productId(), i.quantity())).toList());
    }

    @GetMapping
    public List<OrderDTO> list(Authentication authentication) {
        return orderService.listOrders(authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/{id}")
    public OrderDTO get(@PathVariable long id, Authentication authentication) {
        return orderService.getOrder(id, authentication.getName(), isAdmin(authentication));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderDTO updateStatus(@PathVariable long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderStatus status = request.status();
        return orderService.updateStatus(id, status);
    }

    private boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}

package com.example.inventory_management.controller;

import com.example.inventory_management.dto.OrderDTO;
import com.example.inventory_management.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/seller")
public class SellerRestController {

    private final OrderService orderService;

    public SellerRestController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('SELLER')")
    public List<OrderDTO> listSellerOrders(Authentication authentication) {
        return orderService.listOrdersForSeller(authentication.getName());
    }
}

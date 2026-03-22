package com.example.inventory_management.service;

import com.example.inventory_management.dto.OrderDTO;
import com.example.inventory_management.entity.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderDTO createOrderFromCart(String buyerUsername);
    OrderDTO createOrder(String buyerUsername, List<CreateItem> items);
    List<OrderDTO> listOrders(String buyerUsername, boolean admin);
    OrderDTO getOrder(long orderId, String username, boolean admin);
    OrderDTO updateStatus(long orderId, OrderStatus status);

    record CreateItem(Long productId, int quantity) {}
}

package com.example.inventory_management.service;

import com.example.inventory_management.dto.OrderDTO;
import com.example.inventory_management.entity.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderDTO createOrderFromCart(String buyerUsername);

    OrderDTO createOrderFromCart(String buyerUsername, String couponCode);

    OrderDTO createOrder(String buyerUsername, List<CreateItem> items);

    OrderDTO createOrder(String buyerUsername, List<CreateItem> items, String couponCode);

    OrderDTO payDemo(long orderId, String buyerUsername);

    List<OrderDTO> listOrders(String buyerUsername, boolean admin);

    List<OrderDTO> listOrdersForSeller(String sellerUsername);

    OrderDTO getOrder(long orderId, String username, boolean admin);

    OrderDTO getOrderForSeller(long orderId, String sellerUsername);

    OrderDTO updateStatus(long orderId, OrderStatus status);

    OrderDTO updateStatusForSeller(long orderId, OrderStatus status, String sellerUsername);

    OrderDTO cancelOrder(long orderId, String buyerUsername);

    record CreateItem(Long productId, int quantity) {}
}

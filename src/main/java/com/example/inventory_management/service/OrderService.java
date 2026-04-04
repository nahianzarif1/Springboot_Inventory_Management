package com.example.inventory_management.service;

import com.example.inventory_management.dto.OrderDTO;
import com.example.inventory_management.entity.OrderStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    OrderDTO createOrderFromCart(String buyerUsername);

    OrderDTO createOrderFromCart(String buyerUsername, String couponCode);

    OrderDTO createOrderFromCart(String buyerUsername, String couponCode, Map<Long, String> sellerIdToCouponCode);

    OrderDTO createOrderFromCart(
            String buyerUsername,
            String couponCode,
            Map<Long, String> sellerIdToCouponCode,
            Map<Long, String> productIdToCouponCode);

    /**
     * Single transaction: build order from cart and clear cart. {@code paymentMethod} {@code COD}
     * leaves status PENDING; other values (e.g. CARD, BKASH) apply demo payment (PAID).
     */
    OrderDTO checkoutFromCartAndPayDemo(String buyerUsername, String paymentMethod);

    OrderDTO createOrder(String buyerUsername, List<CreateItem> items);

    OrderDTO createOrder(String buyerUsername, List<CreateItem> items, String couponCode);

    OrderDTO createOrder(String buyerUsername, List<CreateItem> items, String couponCode, Map<Long, String> sellerIdToCouponCode);

    OrderDTO createOrder(
            String buyerUsername,
            List<CreateItem> items,
            String couponCode,
            Map<Long, String> sellerIdToCouponCode,
            Map<Long, String> productIdToCouponCode);

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

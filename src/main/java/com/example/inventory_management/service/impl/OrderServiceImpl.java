package com.example.inventory_management.service.impl;

import com.example.inventory_management.dto.OrderDTO;
import com.example.inventory_management.entity.*;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.InsufficientStockException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.CartItemRepository;
import com.example.inventory_management.repository.OrderRepository;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public OrderDTO createOrderFromCart(String buyerUsername) {
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        List<CartItem> cart = cartItemRepository.findByBuyer(buyer);
        if (cart.isEmpty()) {
            throw new ConflictException("Cart is empty");
        }

        return createOrder(buyerUsername, cart.stream().map(ci -> new CreateItem(ci.getProduct().getId(), ci.getQuantity())).toList());
    }

    @Override
    @Transactional
    public OrderDTO createOrder(String buyerUsername, List<CreateItem> items) {
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        if (items == null || items.isEmpty()) {
            throw new ConflictException("Order items required");
        }

        Order order = Order.builder().buyer(buyer).status(OrderStatus.CREATED).build();

        for (CreateItem reqItem : items) {
            if (reqItem.quantity() <= 0) {
                throw new ConflictException("Quantity must be positive");
            }
            Product product = productRepository.findById(reqItem.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid product"));

            if (reqItem.quantity() > product.getStockQuantity()) {
                throw new InsufficientStockException("Ordering more than stock");
            }

            // decrement stock
            product.setStockQuantity(product.getStockQuantity() - reqItem.quantity());

            OrderItem oi = OrderItem.builder()
                    .product(product)
                    .quantity(reqItem.quantity())
                    .unitPrice(product.getPrice())
                    .build();
            order.addItem(oi);
        }

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByBuyer(buyer);

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> listOrders(String buyerUsername, boolean admin) {
        if (admin) {
            return orderRepository.findAll().stream().map(this::toDto).toList();
        }
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        return orderRepository.findByBuyer(buyer).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrder(long orderId, String username, boolean admin) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!admin && !o.getBuyer().getUsername().equalsIgnoreCase(username)) {
            throw new ConflictException("Cannot access other user's order");
        }
        return toDto(o);
    }

    @Override
    @Transactional
    public OrderDTO updateStatus(long orderId, OrderStatus status) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (o.getStatus() == OrderStatus.SHIPPED && status == OrderStatus.CANCELED) {
            throw new ConflictException("Cannot cancel after shipped");
        }

        o.setStatus(status);
        return toDto(o);
    }

    private OrderDTO toDto(Order o) {
        return new OrderDTO(
                o.getId(),
                o.getBuyer().getId(),
                o.getBuyer().getUsername(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getItems().stream()
                        .map(i -> new OrderDTO.OrderItemDTO(i.getProduct().getId(), i.getProduct().getName(), i.getQuantity()))
                        .toList()
        );
    }
}

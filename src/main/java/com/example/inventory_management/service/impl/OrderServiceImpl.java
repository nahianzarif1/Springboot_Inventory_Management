package com.example.inventory_management.service.impl;

import com.example.inventory_management.dto.OrderDTO;
import com.example.inventory_management.entity.CartItem;
import com.example.inventory_management.entity.Order;
import com.example.inventory_management.entity.OrderItem;
import com.example.inventory_management.entity.OrderStatus;
import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.InsufficientStockException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.CartItemRepository;
import com.example.inventory_management.repository.OrderRepository;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.InventoryLogService;
import com.example.inventory_management.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryLogService inventoryLogService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            CartItemRepository cartItemRepository,
            InventoryLogService inventoryLogService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.inventoryLogService = inventoryLogService;
    }

    @Override
    @Transactional
    public OrderDTO createOrderFromCart(String buyerUsername) {
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        List<CartItem> cart = cartItemRepository.findByBuyer(buyer);
        if (cart.isEmpty()) {
            throw new ConflictException("Cart is empty");
        }

        return createOrder(buyerUsername, cart.stream()
                .map(ci -> new CreateItem(ci.getProduct().getId(), ci.getQuantity()))
                .toList());
    }

    @Override
    @Transactional
    public OrderDTO createOrder(String buyerUsername, List<CreateItem> items) {
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        if (items == null || items.isEmpty()) {
            throw new ConflictException("Order items required");
        }

        Order order = Order.builder().buyer(buyer).status(OrderStatus.PENDING).totalPrice(BigDecimal.ZERO).build();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateItem reqItem : items) {
            if (reqItem.quantity() <= 0) {
                throw new ConflictException("Quantity must be positive");
            }
            Product product = productRepository.findById(reqItem.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid product"));

            if (reqItem.quantity() > product.getStockQuantity()) {
                throw new InsufficientStockException("Ordering more than stock");
            }

            int oldStock = product.getStockQuantity();
            product.setStockQuantity(oldStock - reqItem.quantity());
            User seller = product.getSeller();
            inventoryLogService.record(product, oldStock, product.getStockQuantity(), seller);

            OrderItem oi = OrderItem.builder()
                    .product(product)
                    .quantity(reqItem.quantity())
                    .unitPrice(product.getPrice())
                    .build();
            order.addItem(oi);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(reqItem.quantity())));
        }

        order.setTotalPrice(total);
        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByBuyer(buyer);

        return toDto(saved);
    }

    @Override
    @Transactional
    public OrderDTO payDemo(long orderId, String buyerUsername) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!o.getBuyer().getUsername().equalsIgnoreCase(buyerUsername)) {
            throw new ConflictException("Cannot pay another user's order");
        }
        if (o.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("Cannot pay a canceled order");
        }
        if (o.getStatus() == OrderStatus.SHIPPED) {
            throw new ConflictException("Order already shipped");
        }
        if (o.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("Order already paid");
        }
        if (o.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Order is not payable");
        }

        o.setStatus(OrderStatus.PAID);
        return toDto(o);
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
    public List<OrderDTO> listOrdersForSeller(String sellerUsername) {
        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        return orderRepository.findDistinctBySellerProducts(seller.getId()).stream().map(this::toDto).toList();
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
    @Transactional(readOnly = true)
    public OrderDTO getOrderForSeller(long orderId, String sellerUsername) {
        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        boolean hasProduct = o.getItems().stream()
                .anyMatch(i -> i.getProduct().getSeller().getId().equals(seller.getId()));
        if (!hasProduct) {
            throw new ConflictException("Order does not include this seller's products");
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

    @Override
    @Transactional
    public OrderDTO updateStatusForSeller(long orderId, OrderStatus status, String sellerUsername) {
        getOrderForSeller(orderId, sellerUsername);
        return updateStatus(orderId, status);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(long orderId, String buyerUsername) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!o.getBuyer().getUsername().equalsIgnoreCase(buyerUsername)) {
            throw new ConflictException("Cannot cancel another user's order");
        }
        if (o.getStatus() == OrderStatus.SHIPPED) {
            throw new ConflictException("Cannot cancel after shipped");
        }
        if (o.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("Order already canceled");
        }

        for (OrderItem item : o.getItems()) {
            Product product = item.getProduct();
            int old = product.getStockQuantity();
            product.setStockQuantity(old + item.getQuantity());
            User seller = product.getSeller();
            inventoryLogService.record(product, old, product.getStockQuantity(), seller);
        }

        o.setStatus(OrderStatus.CANCELED);
        return toDto(o);
    }

    private OrderDTO toDto(Order o) {
        return new OrderDTO(
                o.getId(),
                o.getBuyer().getId(),
                o.getBuyer().getUsername(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO,
                o.getItems().stream()
                        .map(i -> new OrderDTO.OrderItemDTO(
                                i.getProduct().getId(),
                                i.getProduct().getName(),
                                i.getQuantity(),
                                i.getUnitPrice()))
                        .toList()
        );
    }
}

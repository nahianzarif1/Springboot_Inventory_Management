package com.example.inventory_management.service.impl;

import com.example.inventory_management.dto.OrderDTO;
import com.example.inventory_management.entity.CartItem;
import com.example.inventory_management.entity.Coupon;
import com.example.inventory_management.entity.Order;
import com.example.inventory_management.entity.OrderItem;
import com.example.inventory_management.entity.OrderStatus;
import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.InsufficientStockException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.CartItemRepository;
import com.example.inventory_management.repository.CouponRepository;
import com.example.inventory_management.repository.OrderRepository;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.CouponService;
import com.example.inventory_management.service.InventoryLogService;
import com.example.inventory_management.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryLogService inventoryLogService;
    private final CouponService couponService;
    private final CouponRepository couponRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            CartItemRepository cartItemRepository,
            InventoryLogService inventoryLogService,
            CouponService couponService,
            CouponRepository couponRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.inventoryLogService = inventoryLogService;
        this.couponService = couponService;
        this.couponRepository = couponRepository;
    }

    @Override
    @Transactional
    public OrderDTO createOrderFromCart(String buyerUsername) {
        return createOrderFromCart(buyerUsername, null, null, null);
    }

    @Override
    @Transactional
    public OrderDTO createOrderFromCart(String buyerUsername, String couponCode) {
        return createOrderFromCart(buyerUsername, couponCode, null, null);
    }

    @Override
    @Transactional
    public OrderDTO createOrderFromCart(String buyerUsername, String couponCode, Map<Long, String> sellerIdToCouponCode) {
        return createOrderFromCart(buyerUsername, couponCode, sellerIdToCouponCode, null);
    }

    @Override
    @Transactional
    public OrderDTO createOrderFromCart(
            String buyerUsername,
            String couponCode,
            Map<Long, String> sellerIdToCouponCode,
            Map<Long, String> productIdToCouponCode) {
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        List<CartItem> cart = cartItemRepository.findByBuyerWithProductDetails(buyer);
        if (cart.isEmpty()) {
            throw new ConflictException("Cart is empty");
        }

        List<CreateItem> items = cart.stream()
                .map(ci -> {
                    Product p = ci.getProduct();
                    if (p == null) {
                        throw new ConflictException("Cart contains an invalid product. Remove it and try again.");
                    }
                    return new CreateItem(p.getId(), ci.getQuantity());
                })
                .toList();

        OrderDTO dto = placeOrder(buyer, items, couponCode, sellerIdToCouponCode, productIdToCouponCode);
        cartItemRepository.deleteByBuyer(buyer);
        return dto;
    }

    @Override
    @Transactional
    public OrderDTO checkoutFromCartAndPayDemo(String buyerUsername, String paymentMethod) {
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        List<CartItem> cart = cartItemRepository.findByBuyerWithProductDetails(buyer);
        if (cart.isEmpty()) {
            throw new ConflictException("Cart is empty");
        }
        List<CreateItem> items = cart.stream()
                .map(ci -> {
                    Product p = ci.getProduct();
                    if (p == null) {
                        throw new ConflictException("Cart contains an invalid product. Remove it and try again.");
                    }
                    return new CreateItem(p.getId(), ci.getQuantity());
                })
                .toList();
        OrderDTO placed = placeOrder(buyer, items, null, null, null);
        cartItemRepository.deleteByBuyer(buyer);
        Order o = orderRepository.findById(placed.id())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!o.getBuyer().getUsername().equalsIgnoreCase(buyerUsername)) {
            throw new ConflictException("Cannot pay another user's order");
        }
        if (isCashOnDelivery(paymentMethod)) {
            return toDto(o);
        }
        applyDemoPayment(o);
        return toDto(o);
    }

    private static boolean isCashOnDelivery(String paymentMethod) {
        return paymentMethod != null && "COD".equalsIgnoreCase(paymentMethod.trim());
    }

    @Override
    @Transactional
    public OrderDTO createOrder(String buyerUsername, List<CreateItem> items) {
        return createOrder(buyerUsername, items, null, null, null);
    }

    @Override
    @Transactional
    public OrderDTO createOrder(String buyerUsername, List<CreateItem> items, String couponCode) {
        return createOrder(buyerUsername, items, couponCode, null, null);
    }

    @Override
    @Transactional
    public OrderDTO createOrder(String buyerUsername, List<CreateItem> items, String couponCode, Map<Long, String> sellerIdToCouponCode) {
        return createOrder(buyerUsername, items, couponCode, sellerIdToCouponCode, null);
    }

    @Override
    @Transactional
    public OrderDTO createOrder(
            String buyerUsername,
            List<CreateItem> items,
            String couponCode,
            Map<Long, String> sellerIdToCouponCode,
            Map<Long, String> productIdToCouponCode) {
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        return placeOrder(buyer, items, couponCode, sellerIdToCouponCode, productIdToCouponCode);
    }

    private OrderDTO placeOrder(
            User buyer,
            List<CreateItem> items,
            String couponCode,
            Map<Long, String> sellerIdToCouponCode,
            Map<Long, String> productIdToCouponCode) {
        if (items == null || items.isEmpty()) {
            throw new ConflictException("Order items required");
        }

        Order order = Order.builder()
                .buyer(buyer)
                .status(OrderStatus.PENDING)
                .subtotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

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
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(reqItem.quantity())));
        }

        Map<Long, String> sellerMap = sellerIdToCouponCode != null ? sellerIdToCouponCode : emptyMap();
        Map<Long, String> productMap = productIdToCouponCode != null ? productIdToCouponCode : emptyMap();
        CouponService.CombinedDiscount combined =
                couponService.calculateCombinedDiscount(couponCode, sellerMap, productMap, items);
        BigDecimal discount = combined.totalDiscount() != null ? combined.totalDiscount() : BigDecimal.ZERO;

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setAppliedCouponCodes(combined.appliedCodesSummary());
        order.setCoupon(combined.singleCouponOrNull());
        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        order.setTotalPrice(total.setScale(2, RoundingMode.HALF_UP));

        Order saved = orderRepository.save(order);

        for (Coupon c : combined.couponsToIncrement()) {
            Coupon fresh = couponRepository.findById(c.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
            fresh.setUsageCount(fresh.getUsageCount() + 1);
        }

        return toDto(saved);
    }

    private void applyDemoPayment(Order o) {
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
    }

    @Override
    @Transactional
    public OrderDTO payDemo(long orderId, String buyerUsername) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!o.getBuyer().getUsername().equalsIgnoreCase(buyerUsername)) {
            throw new ConflictException("Cannot pay another user's order");
        }
        applyDemoPayment(o);
        return toDto(o);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> listOrders(String buyerUsername, boolean admin) {
        if (admin) {
            return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
        }
        return userRepository.findByUsernameIgnoreCase(buyerUsername)
                .map(buyer -> orderRepository.findByBuyerOrderByCreatedAtDesc(buyer).stream()
                        .map(this::toDto)
                        .toList())
                .orElseGet(List::of);
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

        if (o.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("Cannot change status of a canceled order");
        }
        if (o.getStatus() == OrderStatus.SHIPPED && status != OrderStatus.SHIPPED) {
            throw new ConflictException("Cannot change status after shipped");
        }

        o.setStatus(status);
        return toDto(o);
    }

    @Override
    @Transactional
    public OrderDTO updateStatusForSeller(long orderId, OrderStatus status, String sellerUsername) {
        getOrderForSeller(orderId, sellerUsername);
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (status != OrderStatus.SHIPPED) {
            throw new ConflictException("Sellers can only mark orders as shipped");
        }
        if (o.getStatus() != OrderStatus.PAID) {
            throw new ConflictException("Only paid orders can be marked shipped");
        }
        o.setStatus(OrderStatus.SHIPPED);
        return toDto(o);
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
        if (o.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Only unpaid pending orders can be canceled");
        }

        for (OrderItem item : o.getItems()) {
            Product product = item.getProduct();
            int old = product.getStockQuantity();
            product.setStockQuantity(old + item.getQuantity());
            User seller = product.getSeller();
            inventoryLogService.record(product, old, product.getStockQuantity(), seller);
        }

        if (o.getDiscountAmount() != null && o.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            decrementCouponUsages(o);
        }

        o.setStatus(OrderStatus.CANCELED);
        return toDto(o);
    }

    private void decrementCouponUsages(Order o) {
        if (o.getAppliedCouponCodes() != null && !o.getAppliedCouponCodes().isBlank()) {
            for (String part : o.getAppliedCouponCodes().split(",")) {
                String code = part.trim();
                if (code.isEmpty()) {
                    continue;
                }
                couponRepository.findByCodeIgnoreCase(code).ifPresent(c -> {
                    if (c.getUsageCount() > 0) {
                        c.setUsageCount(c.getUsageCount() - 1);
                    }
                });
            }
            return;
        }
        if (o.getCoupon() != null) {
            couponRepository.findById(o.getCoupon().getId()).ifPresent(c -> {
                if (c.getUsageCount() > 0) {
                    c.setUsageCount(c.getUsageCount() - 1);
                }
            });
        }
    }

    private OrderDTO toDto(Order o) {
        BigDecimal discount = o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal total = o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal subtotal = o.getSubtotal() != null ? o.getSubtotal() : total.add(discount);
        String couponCode = o.getAppliedCouponCodes() != null && !o.getAppliedCouponCodes().isBlank()
                ? o.getAppliedCouponCodes()
                : (o.getCoupon() != null ? o.getCoupon().getCode() : null);

        return new OrderDTO(
                o.getId(),
                o.getBuyer().getId(),
                o.getBuyer().getUsername(),
                o.getStatus(),
                o.getCreatedAt(),
                subtotal,
                discount,
                couponCode,
                o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO,
                o.getItems().stream()
                        .map(i -> {
                            Product p = i.getProduct();
                            if (p == null) {
                                return new OrderDTO.OrderItemDTO(0L, "(product removed)", i.getQuantity(), i.getUnitPrice());
                            }
                            return new OrderDTO.OrderItemDTO(
                                    p.getId(),
                                    p.getName() != null ? p.getName() : "—",
                                    i.getQuantity(),
                                    i.getUnitPrice());
                        })
                        .toList()
        );
    }
}

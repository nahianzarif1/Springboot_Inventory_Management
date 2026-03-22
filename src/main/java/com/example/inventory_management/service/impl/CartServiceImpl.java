package com.example.inventory_management.service.impl;

import com.example.inventory_management.entity.CartItem;
import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.InsufficientStockException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.CartItemRepository;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void addToCart(String buyerUsername, long productId, int quantity) {
        if (quantity <= 0) {
            throw new ConflictException("Quantity must be positive");
        }

        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStockQuantity() <= 0) {
            throw new InsufficientStockException("Cannot add out-of-stock product");
        }

        CartItem item = cartItemRepository.findByBuyerIdAndProductId(buyer.getId(), product.getId())
                .orElse(null);

        if (item == null) {
            int finalQty = Math.min(quantity, product.getStockQuantity());
            item = CartItem.builder()
                    .buyer(buyer)
                    .product(product)
                    .quantity(finalQty)
                    .build();
            cartItemRepository.save(item);
            return;
        }

        // Adding same product twice -> increment but don't exceed stock
        int newQty = item.getQuantity() + quantity;
        if (newQty > product.getStockQuantity()) {
            throw new InsufficientStockException("Cannot add more than available stock");
        }
        item.setQuantity(newQty);
    }

    @Override
    @Transactional
    public void removeFromCart(String buyerUsername, long productId) {
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        CartItem item = cartItemRepository.findByBuyerIdAndProductId(buyer.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepository.delete(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCart(String buyerUsername) {
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        return cartItemRepository.findByBuyer(buyer);
    }

    @Override
    @Transactional
    public void clearCart(String buyerUsername) {
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        cartItemRepository.deleteByBuyer(buyer);
    }
}

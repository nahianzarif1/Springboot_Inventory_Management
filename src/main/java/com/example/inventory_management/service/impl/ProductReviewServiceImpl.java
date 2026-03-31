package com.example.inventory_management.service.impl;

import com.example.inventory_management.dto.review.CreateProductReviewRequest;
import com.example.inventory_management.dto.review.ProductReviewDTO;
import com.example.inventory_management.entity.OrderStatus;
import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.ProductReview;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.OrderItemRepository;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.ProductReviewRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.ProductReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductReviewServiceImpl implements ProductReviewService {

    private static final List<OrderStatus> ELIGIBLE_ORDER_STATUSES = List.of(OrderStatus.PAID, OrderStatus.SHIPPED);

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductReviewServiceImpl(
            ProductReviewRepository productReviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderItemRepository orderItemRepository) {
        this.productReviewRepository = productReviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public ProductReviewDTO createOrUpdate(long productId, String buyerUsername, CreateProductReviewRequest request) {
        if (request == null) {
            throw new ConflictException("Review payload required");
        }
        if (request.rating() < 1 || request.rating() > 5) {
            throw new ConflictException("Rating must be between 1 and 5");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        User buyer = userRepository.findByUsernameIgnoreCase(buyerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        boolean eligible = orderItemRepository.buyerHasPurchasedProduct(buyerUsername, productId, ELIGIBLE_ORDER_STATUSES);
        if (!eligible) {
            throw new ConflictException("You can only review products you have purchased (paid orders)");
        }

        ProductReview review = productReviewRepository.findByProductIdAndBuyerId(product.getId(), buyer.getId())
                .orElseGet(() -> ProductReview.builder().product(product).buyer(buyer).build());

        review.setRating(request.rating());
        String comment = request.comment() != null ? request.comment().trim() : null;
        review.setComment(comment != null && !comment.isBlank() ? comment : null);

        ProductReview saved = productReviewRepository.save(review);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReviewDTO> listForProduct(long productId) {
        // Ensure product exists (clear 404 vs empty list)
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        return productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(long productId, long reviewId, String username, boolean admin) {
        ProductReview r = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (r.getProduct() == null || r.getProduct().getId() == null || r.getProduct().getId() != productId) {
            throw new ConflictException("Review does not belong to product");
        }

        if (!admin && (r.getBuyer() == null || !r.getBuyer().getUsername().equalsIgnoreCase(username))) {
            throw new ConflictException("Cannot delete another user's review");
        }

        productReviewRepository.delete(r);
    }

    private ProductReviewDTO toDto(ProductReview r) {
        return new ProductReviewDTO(
                r.getId(),
                r.getProduct() != null ? r.getProduct().getId() : null,
                r.getBuyer() != null ? r.getBuyer().getId() : null,
                r.getBuyer() != null ? r.getBuyer().getUsername() : null,
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}


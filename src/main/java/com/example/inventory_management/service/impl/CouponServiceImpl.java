package com.example.inventory_management.service.impl;

import com.example.inventory_management.dto.coupon.CouponDTO;
import com.example.inventory_management.dto.coupon.CreateCouponRequest;
import com.example.inventory_management.entity.Coupon;
import com.example.inventory_management.entity.DiscountType;
import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.CouponRepository;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.CouponService;
import com.example.inventory_management.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CouponServiceImpl(
            CouponRepository couponRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public CouponDTO create(String sellerUsername, CreateCouponRequest request) {
        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        String code = request.code().trim().toUpperCase();
        if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ConflictException("Coupon code already exists");
        }
        if (!request.expiresAt().isAfter(Instant.now())) {
            throw new ConflictException("Expiry must be in the future");
        }

        if (request.discountType() == DiscountType.PERCENT) {
            if (request.discountValue().compareTo(BigDecimal.ZERO) <= 0
                    || request.discountValue().compareTo(new BigDecimal("100")) > 0) {
                throw new ConflictException("Percent discount must be between 0 and 100");
            }
        } else {
            if (request.discountValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ConflictException("Fixed discount must be positive");
            }
        }

        Integer limit = request.usageLimit();
        if (limit != null && limit < 1) {
            throw new ConflictException("Usage limit must be at least 1 or empty for unlimited");
        }

        Coupon saved = couponRepository.save(Coupon.builder()
                .seller(seller)
                .code(code)
                .discountType(request.discountType())
                .discountValue(request.discountValue().setScale(2, RoundingMode.HALF_UP))
                .expiresAt(request.expiresAt())
                .usageLimit(limit)
                .usageCount(0)
                .active(true)
                .build());

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponDTO> listForSeller(String sellerUsername) {
        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        return couponRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deactivate(long couponId, String sellerUsername) {
        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        Coupon c = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        if (!c.getSeller().getId().equals(seller.getId())) {
            throw new ConflictException("Not your coupon");
        }
        c.setActive(false);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountCalculation calculateDiscount(String couponCodeOrBlank, List<OrderService.CreateItem> items) {
        if (couponCodeOrBlank == null || couponCodeOrBlank.isBlank()) {
            return DiscountCalculation.none();
        }
        String code = couponCodeOrBlank.trim().toUpperCase();
        Coupon c = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid coupon code"));

        assertCouponUsable(c);

        Long sellerId = c.getSeller().getId();
        BigDecimal eligible = eligibleSubtotalForSeller(sellerId, items);

        if (eligible.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("This coupon applies only to products from this seller; add those items to your cart");
        }

        BigDecimal discount = computeDiscountAmount(c, eligible);

        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return DiscountCalculation.none();
        }

        return new DiscountCalculation(discount, c);
    }

    @Override
    @Transactional(readOnly = true)
    public CombinedDiscount calculateCombinedDiscount(
            String globalCouponCodeOrBlank,
            Map<Long, String> sellerIdToCouponCode,
            Map<Long, String> productIdToCouponCode,
            List<OrderService.CreateItem> items) {
        Map<Long, String> effectiveSeller = new LinkedHashMap<>();
        if (globalCouponCodeOrBlank != null && !globalCouponCodeOrBlank.isBlank()) {
            String gc = globalCouponCodeOrBlank.trim().toUpperCase();
            Coupon g = couponRepository.findByCodeIgnoreCase(gc)
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid coupon code"));
            effectiveSeller.putIfAbsent(g.getSeller().getId(), g.getCode());
        }
        if (sellerIdToCouponCode != null) {
            sellerIdToCouponCode.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    effectiveSeller.put(k, v.trim().toUpperCase());
                }
            });
        }

        Map<Long, String> prodMap = productIdToCouponCode != null ? productIdToCouponCode : Map.of();

        Map<Long, BigDecimal> eligibleByCouponId = new LinkedHashMap<>();
        Map<Long, Coupon> couponById = new LinkedHashMap<>();

        for (OrderService.CreateItem ci : items) {
            Product p = productRepository.findById(ci.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid product"));
            if (p.getSeller() == null) {
                continue;
            }
            Long sellerId = p.getSeller().getId();
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(ci.quantity()));

            String code = null;
            String pc = prodMap.get(ci.productId());
            if (pc != null && !pc.isBlank()) {
                code = pc.trim().toUpperCase();
            } else {
                code = effectiveSeller.get(sellerId);
            }
            if (code == null || code.isBlank()) {
                continue;
            }

            Coupon c = couponRepository.findByCodeIgnoreCase(code)
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid coupon code"));
            if (!c.getSeller().getId().equals(sellerId)) {
                throw new ConflictException("Coupon does not apply to this product or seller");
            }

            eligibleByCouponId.merge(c.getId(), line, BigDecimal::add);
            couponById.putIfAbsent(c.getId(), c);
        }

        if (eligibleByCouponId.isEmpty()) {
            return CombinedDiscount.none();
        }

        BigDecimal total = BigDecimal.ZERO;
        List<Coupon> toIncrement = new ArrayList<>();
        List<String> codeLabels = new ArrayList<>();
        Set<Long> seenCouponIds = new LinkedHashSet<>();

        for (Map.Entry<Long, BigDecimal> e : eligibleByCouponId.entrySet()) {
            Coupon c = couponById.get(e.getKey());
            assertCouponUsable(c);
            BigDecimal eligible = e.getValue();
            if (eligible.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal d = computeDiscountAmount(c, eligible);
            if (d.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            total = total.add(d);
            if (seenCouponIds.add(c.getId())) {
                toIncrement.add(c);
                codeLabels.add(c.getCode());
            }
        }

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return CombinedDiscount.none();
        }

        String summary = String.join(", ", codeLabels);
        Coupon single = toIncrement.size() == 1 ? toIncrement.get(0) : null;
        return new CombinedDiscount(total, toIncrement, summary, single);
    }

    private void assertCouponUsable(Coupon c) {
        if (!c.isActive()) {
            throw new ConflictException("Coupon is inactive");
        }
        if (c.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("Coupon has expired");
        }
        if (c.getUsageLimit() != null && c.getUsageCount() >= c.getUsageLimit()) {
            throw new ConflictException("Coupon usage limit reached");
        }
    }

    private BigDecimal eligibleSubtotalForSeller(Long sellerId, List<OrderService.CreateItem> items) {
        BigDecimal eligible = BigDecimal.ZERO;
        for (OrderService.CreateItem ci : items) {
            Product p = productRepository.findById(ci.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid product"));
            if (p.getSeller() != null && p.getSeller().getId().equals(sellerId)) {
                eligible = eligible.add(p.getPrice().multiply(BigDecimal.valueOf(ci.quantity())));
            }
        }
        return eligible;
    }

    private BigDecimal computeDiscountAmount(Coupon c, BigDecimal eligible) {
        if (c.getDiscountType() == DiscountType.PERCENT) {
            return eligible.multiply(c.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return c.getDiscountValue().min(eligible).setScale(2, RoundingMode.HALF_UP);
    }

    private CouponDTO toDto(Coupon c) {
        return new CouponDTO(
                c.getId(),
                c.getCode(),
                c.getDiscountType(),
                c.getDiscountValue(),
                c.getExpiresAt(),
                c.getUsageLimit(),
                c.getUsageCount(),
                c.isActive(),
                c.getCreatedAt()
        );
    }
}

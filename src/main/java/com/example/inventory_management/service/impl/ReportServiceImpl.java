package com.example.inventory_management.service.impl;

import com.example.inventory_management.entity.OrderStatus;
import com.example.inventory_management.repository.OrderItemRepository;
import com.example.inventory_management.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final OrderItemRepository orderItemRepository;

    public ReportServiceImpl(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal totalSales() {
        BigDecimal v = orderItemRepository.totalSalesExcludingCanceled(OrderStatus.CANCELED);
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopProductRow> topSellingProducts(int limit) {
        return orderItemRepository.topSellingProducts(OrderStatus.CANCELED, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(row -> new TopProductRow(
                        (Long) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerPerformanceRow> sellerPerformance() {
        return orderItemRepository.sellerPerformance(OrderStatus.CANCELED).stream()
                .map(row -> new SellerPerformanceRow(
                        (Long) row[0],
                        (String) row[1],
                        (BigDecimal) row[2]))
                .toList();
    }
}

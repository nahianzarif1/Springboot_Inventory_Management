package com.example.inventory_management.service;

import java.math.BigDecimal;
import java.util.List;

public interface ReportService {
    BigDecimal totalSales();

    List<TopProductRow> topSellingProducts(int limit);

    List<SellerPerformanceRow> sellerPerformance();

    record TopProductRow(Long productId, String productName, long unitsSold) {}

    record SellerPerformanceRow(Long sellerId, String sellerUsername, BigDecimal revenue) {}
}

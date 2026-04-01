package com.example.inventory_management.repository;

import com.example.inventory_management.entity.OrderItem;
import com.example.inventory_management.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT COALESCE(SUM(i.quantity * i.unitPrice), 0) FROM OrderItem i JOIN i.order o WHERE o.status <> :canceled")
    BigDecimal totalSalesExcludingCanceled(@Param("canceled") OrderStatus canceled);

    @Query("""
            SELECT i.product.id, i.product.name, SUM(i.quantity) FROM OrderItem i JOIN i.order o
            WHERE o.status <> :canceled
            GROUP BY i.product.id, i.product.name
            ORDER BY SUM(i.quantity) DESC
            """)
    List<Object[]> topSellingProducts(@Param("canceled") OrderStatus canceled, Pageable pageable);

    @Query("""
            SELECT p.seller.id, p.seller.username, COALESCE(SUM(i.quantity * i.unitPrice), 0)
            FROM OrderItem i JOIN i.product p JOIN i.order o
            WHERE o.status <> :canceled
            GROUP BY p.seller.id, p.seller.username
            ORDER BY COALESCE(SUM(i.quantity * i.unitPrice), 0) DESC
            """)
    List<Object[]> sellerPerformance(@Param("canceled") OrderStatus canceled);

    boolean existsByProduct_Id(Long productId);

    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM OrderItem i JOIN i.order o
            WHERE LOWER(o.buyer.username) = LOWER(:buyerUsername)
              AND i.product.id = :productId
              AND o.status IN :eligibleStatuses
            """)
    boolean buyerHasPurchasedProduct(@Param("buyerUsername") String buyerUsername,
                                    @Param("productId") long productId,
                                    @Param("eligibleStatuses") List<OrderStatus> eligibleStatuses);
}

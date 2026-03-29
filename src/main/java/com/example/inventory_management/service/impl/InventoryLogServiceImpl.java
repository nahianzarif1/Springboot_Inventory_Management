package com.example.inventory_management.service.impl;

import com.example.inventory_management.entity.InventoryLog;
import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.repository.InventoryLogRepository;
import com.example.inventory_management.service.InventoryLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryLogServiceImpl implements InventoryLogService {

    private final InventoryLogRepository inventoryLogRepository;

    public InventoryLogServiceImpl(InventoryLogRepository inventoryLogRepository) {
        this.inventoryLogRepository = inventoryLogRepository;
    }

    @Override
    @Transactional
    public void record(Product product, int oldQuantity, int newQuantity, User changedBy) {
        if (oldQuantity == newQuantity) {
            return;
        }
        inventoryLogRepository.save(InventoryLog.builder()
                .product(product)
                .oldQuantity(oldQuantity)
                .newQuantity(newQuantity)
                .changedBy(changedBy)
                .build());
    }
}

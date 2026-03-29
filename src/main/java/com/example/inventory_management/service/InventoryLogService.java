package com.example.inventory_management.service;

import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.User;

public interface InventoryLogService {
    void record(Product product, int oldQuantity, int newQuantity, User changedBy);
}

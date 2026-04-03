package com.example.inventory_management.entity;

public enum DiscountType {
    /** Percentage 1–100 applied to eligible subtotal */
    PERCENT,
    /** Fixed amount off eligible subtotal (cannot exceed eligible subtotal) */
    FIXED
}

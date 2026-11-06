package com.eldercare.backend.nutrition.api.dto;

/** Trạng thái phiếu bữa ăn (api-011 KitchenMealSlip.status). */
public enum MealSlipStatus {
    CLOSED,
    PREPARED,
    DELIVERED,
    HAS_DISCREPANCY,
    RECEIVED
}

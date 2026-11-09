package com.eldercare.backend.nutrition.internal.catalog.domain;

/** Trạng thái món ăn (api-011 {@code DishStatus}; bảng trạng thái món của spec, Q-308). */
public enum DishStatus {
    DRAFT,
    PENDING_CONFIRMATION,
    ACTIVE,
    INACTIVE,
    CANCELLED
}

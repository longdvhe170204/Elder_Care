package com.eldercare.backend.nutrition.internal.catalog.domain;

/** Trạng thái món ăn (api-011 {@code DishStatus}; bảng trạng thái món của spec, Q-445). */
public enum DishStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    CANCELLED
}

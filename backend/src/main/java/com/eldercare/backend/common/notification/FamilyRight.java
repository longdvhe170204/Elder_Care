package com.eldercare.backend.common.notification;

/**
 * Sáu quyền riêng của người thân theo từng quan hệ ({@code QuyenNguoiThan}; 012 FR-007). 009 chọn người nhận và lọc nội
 * dung theo bốn quyền đầu (009 FR-013, Q-96); {@code PICKUP_ALLOWED}, {@code REQUEST_SERVICES} thêm 2026-11-03 khi cài 012.
 */
public enum FamilyRight {
    VIEW_HEALTH,
    VIEW_COSTS,
    REGISTER_VISITS,
    RECEIVE_URGENT,
    PICKUP_ALLOWED,
    REQUEST_SERVICES
}

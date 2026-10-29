package com.eldercare.backend.resident.api.dto;

import java.util.Optional;

/**
 * Mục sức khỏe Hiệu lực cho 006, 007, 011.
 *
 * @param allergenId có khi là dị ứng chọn từ danh mục
 * @param freeText có khi là dị ứng khác, bệnh nền, tiền sử
 * @param notAutoChecked dị ứng khác nhập tay, hệ thống không kiểm tra tự động
 * @param pendingCheck chờ 006 hoặc 011 kiểm tra (BR-M01-12)
 */
public record HealthItemView(
        long id,
        long residentId,
        HealthItemType type,
        Optional<Long> allergenId,
        Optional<String> freeText,
        boolean notAutoChecked,
        boolean pendingCheck) {}

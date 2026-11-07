package com.eldercare.backend.belongings.api.dto;

import java.util.List;

/**
 * Điều kiện (b) kết thúc lưu trú (FR-025, research K-08): Đạt khi không còn đồ Đang giữ, Đang được sử dụng, Hư hỏng; đồ
 * Thất lạc có sự cố mở liệt kê riêng vì chặn qua điều kiện (d).
 */
public record BelongingEndingConditionView(
        boolean met, List<Long> blockingBelongingIds, List<Long> lostWithOpenIncidentBelongingIds) {

    public BelongingEndingConditionView {
        blockingBelongingIds = List.copyOf(blockingBelongingIds);
        lostWithOpenIncidentBelongingIds = List.copyOf(lostWithOpenIncidentBelongingIds);
    }
}

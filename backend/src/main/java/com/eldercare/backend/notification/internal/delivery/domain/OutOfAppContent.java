package com.eldercare.backend.notification.internal.delivery.domain;

import com.eldercare.backend.common.notification.HealthFields;
import java.util.Map;
import java.util.Optional;

/**
 * Kiểm nội dung kênh ngoài ứng dụng ({@code KiemTraNoiDungKenhNgoai}; FR-019, FR-020, research B-05): nội dung rút gọn
 * dựng từ tham số hiển thị có khóa thuộc danh mục trường sức khỏe thì không gửi ra ngoài; thiếu nội dung rút gọn thì
 * không có gì để gửi.
 */
public final class OutOfAppContent {

    private OutOfAppContent() {}

    public static boolean invalid(Map<String, Object> displayParams, Optional<String> shortContent) {
        if (shortContent == null || shortContent.filter(s -> !s.isBlank()).isEmpty()) {
            return true;
        }
        return displayParams != null && displayParams.keySet().stream().anyMatch(HealthFields.KEYS::contains);
    }
}

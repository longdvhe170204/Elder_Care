package com.eldercare.backend.common.notification;

import java.util.Set;

/**
 * Danh mục trường thông tin sức khỏe chung ({@code TRUONG_SUC_KHOE}; 009 FR-020, research B-05): khóa
 * {@link NotificationRequest#displayParams()} cấm dùng để dựng nội dung rút gọn gửi qua kênh ngoài ứng dụng.
 */
public final class HealthFields {

    /** Chẩn đoán, dị ứng, bệnh nền, chỉ số, thuốc, kết quả đánh giá, nguyện vọng, nội dung sự cố. */
    public static final Set<String> KEYS = Set.of(
            "diagnosis",
            "allergies",
            "underlyingConditions",
            "vitalSigns",
            "medication",
            "assessmentResult",
            "wishes",
            "incidentContent");

    private HealthFields() {}
}

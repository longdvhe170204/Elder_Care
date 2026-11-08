package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.NotificationLevel;
import java.util.List;

/**
 * 014 bảng thông báo dòng "Buổi bị hủy hoặc dời; đăng ký bị hủy tự động" (FR-009, FR-012, FR-013, FR-021, FR-021a,
 * FR-022): một sự kiện mỗi buổi; người nhận là người phụ trách buổi, người đã đăng ký (đăng ký nguồn thường xuyên: Trưởng
 * tầng của hoạt động), Trưởng tầng khi khoanh vùng, nghi nhiễm; Nhẹ. {@code cause} là mã lý do (glossary 20.6).
 */
public record ActivityRegistrationsCancelled(
        long sessionId, List<Long> registrationIds, String cause, CareEventContext context) implements CareNotice {
    public ActivityRegistrationsCancelled {
        registrationIds = List.copyOf(registrationIds);
    }

    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }
}

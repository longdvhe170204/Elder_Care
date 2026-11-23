package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/**
 * T004/C-13: sự kiện thông báo theo hợp đồng 005 mục 1.11. Tiêu đề nói lý do cần lập công việc để Trưởng tầng biết có
 * người cao tuổi vào tầng (BF-01 bước 15 nối BF-02 bước 1); không chứa tên hay thông tin sức khỏe (009 FR-034).
 */
public record TaskPlanningNeeded(long residentId, String reason, Instant from, CareEventContext context)
        implements CareNotice {
    private static final Map<String, String> TITLES = Map.of(
            "ADMISSION_COMPLETED", "Có người cao tuổi mới vào tầng, cần lập công việc",
            "FLOOR_TRANSFER", "Có người cao tuổi chuyển đến tầng, cần lập công việc",
            "RETURN", "Người cao tuổi đã quay lại, cần lập công việc",
            "ABSENCE_RECALCULATED", "Khoảng vắng đã đính chính, cần xem lại công việc",
            "DAY_CARE_ARRIVAL", "Người bán trú có mặt ngoài lịch, xem có cần công việc phát sinh");

    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }

    @Override
    public Optional<Long> notificationResidentId() {
        return Optional.of(residentId);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        String title = TITLES.getOrDefault(reason, "Cần lập công việc cho người cao tuổi");
        return List.of(CareEventNotifications.titled(this, level(), context().recipients(), eventKey(), title));
    }
}

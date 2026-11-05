package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.NotificationLevel;

/**
 * 014 FR-064: báo người thực hiện công việc gốc khi công việc bị ghi Không đạt (kèm công việc làm lại). Không báo khi công
 * việc được chọn kiểm tra (FR-059); thông báo không mang chi tiết sức khỏe.
 */
public record QualityCheckFailedNotice(
        long itemId, long originalTaskId, long performerStaffId, CareEventContext context) implements CareNotice {
    @Override
    public NotificationLevel level() {
        return NotificationLevel.MEDIUM;
    }
}

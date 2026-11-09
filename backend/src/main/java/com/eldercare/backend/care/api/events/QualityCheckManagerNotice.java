package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.NotificationLevel;

/**
 * 014 FR-063: báo Quản lý viện một lần cho mỗi danh sách có mục Quá hạn kiểm tra ({@code OVERDUE_ITEMS}) và mỗi ca có
 * công việc đủ điều kiện kết thúc không có danh sách ({@code NO_CHECKLIST}).
 */
public record QualityCheckManagerNotice(long shiftId, long floorAreaId, String cause, CareEventContext context)
        implements CareNotice {
    @Override
    public NotificationLevel level() {
        return NotificationLevel.MEDIUM;
    }
}

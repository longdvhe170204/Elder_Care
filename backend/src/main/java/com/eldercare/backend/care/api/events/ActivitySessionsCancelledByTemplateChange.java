package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.NotificationLevel;
import java.util.List;

/** 014 FR-009 (bảng thông báo): buổi bị hủy do đổi mẫu, Trưởng tầng của hoạt động tạo lại theo mẫu mới; Nhẹ. */
public record ActivitySessionsCancelledByTemplateChange(
        long activityId, List<Long> sessionIds, CareEventContext context) implements CareNotice {
    public ActivitySessionsCancelledByTemplateChange {
        sessionIds = List.copyOf(sessionIds);
    }

    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }
}

package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.NotificationLevel;

/** 014 FR-014a (bảng thông báo): hết ngày, buổi có "Không ghi nhận" hoặc "Không điểm danh"; Trung bình. */
public record ActivitySessionDayEnded(long sessionId, boolean noAttendance, CareEventContext context)
        implements CareNotice {
    @Override
    public NotificationLevel level() {
        return NotificationLevel.MEDIUM;
    }
}

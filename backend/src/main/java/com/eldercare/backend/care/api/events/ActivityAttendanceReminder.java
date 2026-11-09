package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.NotificationLevel;

/** 014 FR-030 (bảng thông báo): buổi chưa Hoàn tất điểm danh sau kết thúc + CFG-M04-06; một lần mỗi buổi; Nhẹ. */
public record ActivityAttendanceReminder(long sessionId, CareEventContext context) implements CareNotice {
    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }
}

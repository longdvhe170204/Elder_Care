package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.NotificationLevel;

/**
 * 014 FR-059a: tại mốc CFG-M09-04 trước kết ca, nhắc người kiểm tra khi ca chưa có danh sách mà đã có công việc đủ điều
 * kiện, hoặc danh sách thiếu tỷ lệ. Chỉ là lời nhắc; không lập danh sách hay thêm mục (Q-306).
 */
public record QualityCheckReminder(long shiftId, long floorAreaId, CareEventContext context) implements CareNotice {
    @Override
    public NotificationLevel level() {
        return NotificationLevel.MEDIUM;
    }
}

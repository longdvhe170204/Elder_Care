package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.NotificationLevel;
import java.util.List;

/** 014 FR-008 (bảng thông báo): người thường xuyên không được đăng ký sẵn, gộp một lần mỗi lệnh; Nhẹ. */
public record RegularParticipantsNotRegistered(long activityId, List<Long> residentIds, CareEventContext context)
        implements CareNotice {
    public RegularParticipantsNotRegistered {
        residentIds = List.copyOf(residentIds);
    }

    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }
}

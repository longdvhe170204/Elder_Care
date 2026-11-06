package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.NotificationLevel;
import java.util.Optional;

/**
 * 014 FR-071 (dòng bổ sung Q-366): nhắc chính người cao tuổi có tài khoản về buổi trong ngày đã có tên (nhóm người nhận
 * người cao tuổi của 009); chỉ phần chung, giờ yên tĩnh do 009 áp.
 */
public record ActivityResidentReminder(long sessionId, long registrationId, long residentId, CareEventContext context)
        implements CareNotice {
    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }

    @Override
    public Optional<Long> notificationResidentId() {
        return Optional.of(residentId);
    }
}

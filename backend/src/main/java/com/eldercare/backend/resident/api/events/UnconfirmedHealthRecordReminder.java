package com.eldercare.backend.resident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Nhắc hồ sơ sức khỏe ban đầu chưa được Bác sĩ xác nhận hoặc còn mục tự khai chưa kiểm chứng; báo Bác sĩ trực và Điều dưỡng
 * phụ trách, mức Nhẹ (Q-400, BR-M01-17). Khóa sự kiện (residentId, reminderNumber).
 */
public record UnconfirmedHealthRecordReminder(long residentId, int reminderNumber, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "UNCONFIRMED_HEALTH_RECORD_REMINDER",
                NotificationLevel.LOW,
                new SourceRecord("M01.RESIDENT", String.valueOf(residentId)),
                "UNCONFIRMED_HEALTH_RECORD_REMINDER:" + residentId + ":" + reminderNumber,
                residentId,
                null,
                List.of(Notices.doctor(), Notices.assignedNurse(residentId)),
                Notices.health("Nhắc lần "
                        + reminderNumber
                        + ": hồ sơ sức khỏe ban đầu chưa được Bác sĩ xác nhận hoặc còn mục tự khai chưa kiểm chứng."),
                "Hồ sơ sức khỏe chưa xác nhận",
                "Có hồ sơ sức khỏe ban đầu chưa xác nhận hoặc còn mục tự khai chưa kiểm chứng.",
                at));
    }
}

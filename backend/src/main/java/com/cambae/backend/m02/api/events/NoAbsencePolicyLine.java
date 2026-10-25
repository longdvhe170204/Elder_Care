package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Không có dòng chính sách vắng cho ngày vắng, áp hệ số 100% và giữ giường; mức Trung bình (FR-053). */
public record NoAbsencePolicyLine(long absenceId, long residentId, LocalDate date, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "NO_ABSENCE_POLICY_LINE",
                new SourceRecord("M02.ABSENCE", String.valueOf(absenceId)),
                "NO_ABSENCE_POLICY_LINE:" + absenceId + ":" + date,
                NotificationLevel.MEDIUM,
                residentId,
                Notices.MANAGER,
                "Không có dòng chính sách vắng",
                "Ngày vắng " + date + " không có dòng chính sách; đang áp hệ số 100% và giữ giường.",
                at));
    }
}

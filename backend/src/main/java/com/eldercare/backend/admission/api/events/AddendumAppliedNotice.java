package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Phụ lục đã áp dụng: báo người đại diện nội dung và ngày hiệu lực, mức Nhẹ, không kèm giá (đề xuất D 2026-11-18, chờ chủ dự
 * án duyệt; team-plan mục 8).
 */
public record AddendumAppliedNotice(
        long addendumId, long residentId, Set<String> changeTypes, LocalDate effectiveDate, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String text = "Phụ lục hợp đồng thay đổi " + RepresentativeNotices.changes(changeTypes) + " có hiệu lực từ "
                + RepresentativeNotices.day(effectiveDate) + ".";
        return List.of(RepresentativeNotices.of(
                "ADDENDUM_APPLIED",
                new SourceRecord("M02.ADDENDUM", String.valueOf(addendumId)),
                "ADDENDUM_APPLIED:" + addendumId,
                residentId,
                "Phụ lục hợp đồng có hiệu lực",
                text,
                at));
    }
}

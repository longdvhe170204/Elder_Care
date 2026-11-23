package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Yêu cầu thay đổi lưu trú liên quan tới gia đình được gửi duyệt (FR-043g, Q-422): báo các người đại diện hiện tại, mức Nhẹ,
 * không kèm giá. {@code confirmerAccountId} là tài khoản của người đại diện đã xác nhận (kèm bằng chứng hoặc đồng ý trên
 * ứng dụng), được bỏ khỏi người nhận (009 bỏ người gây ra thông báo mức Nhẹ).
 */
public record ResidencyChangeSubmittedNotice(
        long requestId,
        long residentId,
        Set<String> changeTypes,
        LocalDate requestedEffectiveDate,
        Long confirmerAccountId,
        Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String text = "Viện đã gửi duyệt yêu cầu thay đổi " + RepresentativeNotices.changes(changeTypes) + " từ ngày "
                + RepresentativeNotices.day(requestedEffectiveDate) + ".";
        NotificationRequest base = RepresentativeNotices.of(
                "RESIDENCY_CHANGE_SUBMITTED",
                new SourceRecord("M02.RESIDENCY_CHANGE", String.valueOf(requestId)),
                "RESIDENCY_CHANGE_SUBMITTED:" + requestId + ":" + at.toEpochMilli(),
                residentId,
                "Yêu cầu điều chỉnh hợp đồng đã gửi duyệt",
                text,
                at);
        return List.of(new NotificationRequest(
                base.sourceModule(),
                base.eventType(),
                base.sourceGroup(),
                base.sourceRecord(),
                base.eventKey(),
                base.level(),
                base.residentId(),
                base.floorAreaId(),
                base.recipientGroups(),
                base.content(),
                base.shortContent(),
                base.title(),
                Optional.ofNullable(confirmerAccountId),
                false,
                base.occurredAt(),
                base.displayParams(),
                base.afterDeath(),
                base.informationSubject(),
                base.q354Case()));
    }
}

package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/** Đặt cọc về Chưa đáp ứng khi người cao tuổi đã lưu trú; báo Quản lý viện, mức Trung bình (FR-032). */
public record DepositNotSatisfiedAfterAdmission(long contractId, long residentId, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "DEPOSIT_NOT_SATISFIED_AFTER_ADMISSION",
                new SourceRecord("M02.CONTRACT", String.valueOf(contractId)),
                "DEPOSIT_NOT_SATISFIED_AFTER_ADMISSION:" + contractId + ":" + at.toEpochMilli(),
                NotificationLevel.MEDIUM,
                residentId,
                Notices.MANAGER,
                "Đặt cọc chưa đáp ứng",
                "Đặt cọc của hợp đồng về Chưa đáp ứng trong khi người cao tuổi đã lưu trú.",
                at));
    }
}

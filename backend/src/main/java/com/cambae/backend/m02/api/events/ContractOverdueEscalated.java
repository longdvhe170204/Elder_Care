package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/** Hợp đồng quá hạn vượt CFG-M02-10; báo Quản lý viện, mức Trung bình (FR-029). */
public record ContractOverdueEscalated(long contractId, long residentId, int days, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "CONTRACT_OVERDUE_ESCALATED",
                new SourceRecord("M02.CONTRACT", String.valueOf(contractId)),
                "CONTRACT_OVERDUE_ESCALATED:" + contractId,
                NotificationLevel.MEDIUM,
                residentId,
                Notices.MANAGER,
                "Hợp đồng quá hạn",
                "Hợp đồng đã quá hạn " + days + " ngày mà chưa có phụ lục gia hạn hay lệnh Kết thúc lưu trú.",
                at));
    }
}

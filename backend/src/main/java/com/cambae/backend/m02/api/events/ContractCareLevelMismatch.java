package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.dto.ContractStatus;
import java.time.Instant;
import java.util.List;

/** Mức của đánh giá đầu vào khác mức hợp đồng chưa kết thúc; báo Hành chính, mức Trung bình (FR-025b, Q-302). */
public record ContractCareLevelMismatch(
        long residentId, CareLevel assessedLevel, CareLevel contractLevel, ContractStatus contractStatus, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "CONTRACT_CARE_LEVEL_MISMATCH",
                new SourceRecord("M01.RESIDENT", String.valueOf(residentId)),
                "CONTRACT_CARE_LEVEL_MISMATCH:" + residentId + ":" + assessedLevel + ":" + contractLevel,
                NotificationLevel.MEDIUM,
                residentId,
                Notices.OFFICE,
                "Mức chăm sóc hợp đồng lệch đánh giá",
                "Mức chăm sóc đánh giá là " + assessedLevel + ", mức hợp đồng " + contractStatus + " là "
                        + contractLevel + ".",
                at));
    }
}

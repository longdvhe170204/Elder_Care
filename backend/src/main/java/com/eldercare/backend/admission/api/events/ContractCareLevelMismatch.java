package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.resident.api.dto.CareLevel;
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

package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Phụ lục còn Chờ ký (FR-043d, Q-420): nhắc Hành chính in phụ lục cho người đại diện ký và ghi nhận bản đã ký, mỗi
 * CFG-M02-17. Mức Nhẹ; nguồn là yêu cầu thay đổi lưu trú để mở đúng màn.
 */
public record AddendumSignatureReminder(long addendumId, long requestId, long residentId, int addendumNo, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "ADDENDUM_SIGNATURE_REMINDER",
                new SourceRecord("M02.RESIDENCY_CHANGE", String.valueOf(requestId)),
                "ADDENDUM_SIGNATURE_REMINDER:" + addendumId + ":" + at.toEpochMilli(),
                NotificationLevel.LOW,
                residentId,
                Notices.OFFICE,
                "Phụ lục số %02d chưa ký".formatted(addendumNo),
                "Phụ lục hợp đồng đã được duyệt nhưng chưa có bản ký của người đại diện. In phụ lục, hẹn người đại diện"
                        + " ký và ghi nhận bản đã ký; chưa ký thì phụ lục chưa có hiệu lực.",
                at));
    }
}

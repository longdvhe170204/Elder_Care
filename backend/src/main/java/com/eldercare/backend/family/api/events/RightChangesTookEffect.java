package com.eldercare.backend.family.api.events;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yêu cầu thay đổi quyền đã có hiệu lực (FR-016, Q-249, Q-267): 009 gửi mức Nhẹ cho các người đại diện Hiệu lực khác
 * của người cao tuổi, đã trừ người lập, người xác nhận và người bị thôi, vừa được thêm (tập xét sau khi có hiệu lực).
 * Loại thông tin "chung", về việc của chính người thân (FR-069, research F-15).
 *
 * @param source bản ghi nguồn: phiếu đăng ký (US1) hoặc yêu cầu (US3)
 * @param summary loại thay đổi, người bị tác động và người xác nhận
 */
public record RightChangesTookEffect(
        long residentId, SourceRecord source, String summary, List<Long> otherRepresentativeIds, Instant at)
        implements HasNotificationRequests {

    public static final String EVENT_TYPE = "RIGHT_CHANGE_EFFECTIVE_OTHER_REPRESENTATIVES";

    public RightChangesTookEffect {
        otherRepresentativeIds = List.copyOf(otherRepresentativeIds);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        if (otherRepresentativeIds.isEmpty()) {
            return List.of();
        }
        return List.of(new NotificationRequest(
                "M10",
                EVENT_TYPE,
                SourceGroup.FAMILY_INFO,
                source,
                source.type() + ":" + source.id() + ":" + EVENT_TYPE,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                otherRepresentativeIds.stream()
                        .<RecipientGroup>map(id -> new RecipientGroup.FamilyMember(id, "REPRESENTATIVE"))
                        .toList(),
                List.of(new ContentPart(InformationType.GENERAL, summary)),
                Optional.of(summary),
                Optional.of("Thay đổi quyền người thân đã có hiệu lực"),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty()));
    }
}

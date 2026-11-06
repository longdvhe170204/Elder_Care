package com.eldercare.backend.family.api.events;

import com.eldercare.backend.common.Role;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thông báo của yêu cầu thay đổi quyền theo bảng FR-069 (BR-M10-07, FR-019; research F-15): loại thông tin "chung", về
 * việc của chính người thân. Người nhận đã tính ở lệnh; Quản lý viện, Hành chính là nhóm vai trò toàn viện.
 *
 * @param representativeIds người đại diện cần xác nhận hoặc được nhắc ({@link Kind#NEEDS_CONFIRMATION}, {@link
 *     Kind#REMINDER})
 * @param creatorFamilyMemberId người thân lập qua cổng ({@link Kind#DECIDED})
 * @param affectedFamilyMemberId người thân bị tác động ({@link Kind#DECIDED})
 * @param sequence số thứ tự lần nhắc, cho khóa sự kiện
 */
public record RightChangeRequestNotice(
        Kind kind,
        long residentId,
        long requestId,
        String summary,
        List<Long> representativeIds,
        Long creatorFamilyMemberId,
        Long affectedFamilyMemberId,
        int sequence,
        Instant at)
        implements HasNotificationRequests {

    public static final String SOURCE_TYPE = "M10.RIGHT_CHANGE_REQUEST";

    public enum Kind {
        /** Cần xác nhận/duyệt: Trung bình, người đại diện và Quản lý viện. */
        NEEDS_CONFIRMATION(
                "RIGHT_CHANGE_NEEDS_CONFIRMATION", NotificationLevel.MEDIUM, "Yêu cầu thay đổi quyền cần xác nhận"),
        /** Có kết quả: Nhẹ, người lập, người bị tác động, Hành chính. */
        DECIDED("RIGHT_CHANGE_DECIDED", NotificationLevel.LOW, "Yêu cầu thay đổi quyền đã có kết quả"),
        /** Quá mốc nhắc CFG-M10-11: Trung bình, người đại diện và Quản lý viện. */
        REMINDER("RIGHT_CHANGE_REMINDER", NotificationLevel.MEDIUM, "Yêu cầu thay đổi quyền đang chờ xác nhận"),
        /** Quá mốc báo CFG-M10-11: Trung bình, Quản lý viện. */
        MANAGER_NOTICE("RIGHT_CHANGE_MANAGER_NOTICE", NotificationLevel.MEDIUM, "Yêu cầu thay đổi quyền chờ quá lâu");

        private final String eventType;
        private final NotificationLevel level;
        private final String title;

        Kind(String eventType, NotificationLevel level, String title) {
            this.eventType = eventType;
            this.level = level;
            this.title = title;
        }

        public String eventType() {
            return eventType;
        }
    }

    public RightChangeRequestNotice {
        representativeIds = List.copyOf(representativeIds);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<RecipientGroup> recipients = new ArrayList<>();
        switch (kind) {
            case NEEDS_CONFIRMATION, REMINDER -> {
                representativeIds.forEach(id -> recipients.add(new RecipientGroup.FamilyMember(id, "REPRESENTATIVE")));
                recipients.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
            }
            case DECIDED -> {
                if (creatorFamilyMemberId != null) {
                    recipients.add(new RecipientGroup.FamilyMember(creatorFamilyMemberId, "CREATOR"));
                }
                if (affectedFamilyMemberId != null && !affectedFamilyMemberId.equals(creatorFamilyMemberId)) {
                    recipients.add(new RecipientGroup.FamilyMember(affectedFamilyMemberId, "AFFECTED"));
                }
                recipients.add(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
            }
            case MANAGER_NOTICE -> recipients.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        }
        SourceRecord source = new SourceRecord(SOURCE_TYPE, String.valueOf(requestId));
        String key = SOURCE_TYPE + ":" + requestId + ":" + kind.eventType + (sequence > 0 ? ":" + sequence : "");
        return List.of(new NotificationRequest(
                "M10",
                kind.eventType,
                SourceGroup.FAMILY_INFO,
                source,
                key,
                Optional.of(kind.level),
                Optional.of(residentId),
                Optional.empty(),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, summary)),
                Optional.of(summary),
                Optional.of(kind.title),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty()));
    }
}

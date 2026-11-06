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
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thông báo của lượt ở lại (012 spec bảng thông báo; FR-040, FR-044, FR-045): lượt cần xác nhận báo Trưởng tầng được
 * giao của tầng, không có thì Quản lý viện (Trung bình); xác nhận, từ chối báo Hành chính và người thân ở lại (Nhẹ, phần
 * chung); nhắc kết thúc khi người cao tuổi rời viện báo Hành chính, Trưởng tầng (Trung bình).
 *
 * @param floorAreaId tầng của người cao tuổi lúc báo; không có thì lượt cần xác nhận báo Quản lý viện
 */
public record FamilyStayNotice(
        long stayId, long residentId, long familyMemberId, Kind kind, Long floorAreaId, Instant at)
        implements HasNotificationRequests {

    public static final String SOURCE_TYPE = "M10.FAMILY_STAY";

    public enum Kind {
        REGISTERED,
        CONFIRMED,
        REJECTED,
        END_REMINDER
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<RecipientGroup> recipients = new ArrayList<>();
        String title;
        NotificationLevel level;
        Optional<InformationSubject> subject = Optional.empty();
        switch (kind) {
            case REGISTERED -> {
                title = "Lượt người thân ở lại cần xác nhận";
                level = NotificationLevel.MEDIUM;
                recipients.add(
                        floorAreaId == null
                                ? new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)
                                : new RecipientGroup.FloorManager(
                                        floorAreaId,
                                        new Substitution.Designated(
                                                new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))));
            }
            case CONFIRMED, REJECTED -> {
                title = kind == Kind.CONFIRMED ? "Lượt ở lại đã được xác nhận" : "Lượt ở lại bị từ chối";
                level = NotificationLevel.LOW;
                recipients.add(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
                recipients.add(new RecipientGroup.FamilyMember(familyMemberId, "FAMILY_STAY_GUEST"));
                subject = Optional.of(InformationSubject.ABOUT_FAMILY_MATTER);
            }
            default -> {
                title = "Nhắc kết thúc lượt người thân ở lại";
                level = NotificationLevel.MEDIUM;
                recipients.add(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
                if (floorAreaId != null) {
                    recipients.add(new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default()));
                }
            }
        }
        String summary =
                kind == Kind.END_REMINDER ? title + ": người cao tuổi đã rời viện, ghi kết thúc lượt ở lại" : title;
        return List.of(new NotificationRequest(
                "M10",
                "FAMILY_STAY_" + kind.name(),
                SourceGroup.FAMILY_INFO,
                new SourceRecord(SOURCE_TYPE, String.valueOf(stayId)),
                SOURCE_TYPE + ":" + stayId + ":" + kind.name() + (kind == Kind.END_REMINDER ? ":" + at : ""),
                Optional.of(level),
                Optional.of(residentId),
                Optional.empty(),
                List.copyOf(recipients),
                List.of(new ContentPart(InformationType.GENERAL, summary)),
                Optional.of(summary),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                subject,
                Optional.empty()));
    }
}

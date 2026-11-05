package com.eldercare.backend.family.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
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
import java.util.Set;

/**
 * Khoanh vùng làm hủy lượt thăm (FR-037): Hành chính và Trưởng tầng của các tầng có người cao tuổi bị ảnh hưởng nhận danh
 * sách lượt đã hủy; lượt đang Đã vào không bị hủy, Hành chính được báo để mời khách ra và ghi giờ ra.
 */
public record VisitIsolationNotice(
        long zoneId, List<Long> cancelledVisitIds, List<Long> checkedInVisitIds, Set<Long> floorAreaIds, Instant at)
        implements HasNotificationRequests {

    public static final String SOURCE_TYPE = "M05.ISOLATION_ZONE";
    private static final String TITLE = "Lượt thăm bị ảnh hưởng do khoanh vùng";

    public VisitIsolationNotice {
        cancelledVisitIds = List.copyOf(cancelledVisitIds);
        checkedInVisitIds = List.copyOf(checkedInVisitIds);
        floorAreaIds = Set.copyOf(floorAreaIds);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        String summary = TITLE + ": " + cancelledVisitIds.size() + " lượt đã hủy"
                + (checkedInVisitIds.isEmpty()
                        ? ""
                        : ", " + checkedInVisitIds.size()
                                + " lượt khách đang ở trong viện, mời khách ra và ghi giờ ra");
        List<RecipientGroup> recipients = new ArrayList<>();
        recipients.add(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        floorAreaIds.stream()
                .sorted()
                .forEach(floor -> recipients.add(new RecipientGroup.FloorManager(floor, new Substitution.Default())));
        return List.of(new NotificationRequest(
                "M10",
                "VISITS_CANCELLED_BY_ISOLATION",
                SourceGroup.FAMILY_INFO,
                new SourceRecord(SOURCE_TYPE, String.valueOf(zoneId)),
                "M10.VISIT_ISOLATION:" + zoneId,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.empty(),
                Optional.empty(),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, summary)),
                Optional.of(summary),
                Optional.of(TITLE),
                Optional.empty(),
                false,
                at,
                Map.of("cancelledVisitIds", cancelledVisitIds, "checkedInVisitIds", checkedInVisitIds),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}

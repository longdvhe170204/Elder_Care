package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Người nhận cảnh báo nút hỗ trợ khẩn cấp (FR-047i, research S-24): Bác sĩ trực (không có thì FR-047b), Người phụ trách
 * ca và nhân viên trong ca của tầng; chưa có tầng thì Người phụ trách ca của mọi tầng đang có ca. Không có nhóm người thân.
 */
final class AssistNotices {
    static final String CREATED = "STAFF_ASSIST_ALERT_CREATED";
    static final String DETAILS = "STAFF_ASSIST_ALERT_DETAILS";

    private AssistNotices() {}

    static List<NotificationRequest> created(
            AlertRecord alert, boolean noOnDutyDoctor, Collection<Long> floorsWithShift, Instant at) {
        var groups = new LinkedHashSet<RecipientGroup>();
        if (noOnDutyDoctor) {
            groups.add(new RecipientGroup.FacilityRole(Role.DOCTOR));
            groups.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        } else {
            groups.add(new RecipientGroup.OnDutyDoctor(new Substitution.Default()));
        }
        if (alert.floorAreaId() != null) {
            groups.addAll(floor(alert.floorAreaId()));
        } else {
            floorsWithShift.forEach(f -> groups.add(new RecipientGroup.ShiftLead(f, new Substitution.Default())));
        }
        return List.of(request(
                alert,
                CREATED,
                "ASSIST_CREATED",
                List.copyOf(groups),
                "Nhân viên vừa bấm nút hỗ trợ khẩn cấp. Mở cảnh báo để tiếp nhận và tới ngay.",
                at));
    }

    /** Người bấm chọn tầng hoặc người cao tuổi sau: gửi thêm nhóm của tầng, nhóm FR-047a của người cao tuổi. */
    static List<NotificationRequest> details(
            AlertRecord alert, Long newFloor, Long residentId, Long residentFloor, Instant at) {
        var groups = new LinkedHashSet<RecipientGroup>();
        if (newFloor != null) groups.addAll(floor(newFloor));
        if (residentId != null) {
            groups.add(new RecipientGroup.AssignedNurse(residentId, new Substitution.Default()));
            if (residentFloor != null) {
                groups.add(new RecipientGroup.ShiftLead(residentFloor, new Substitution.Default()));
                groups.add(new RecipientGroup.FloorManager(residentFloor, new Substitution.Default()));
            }
        }
        if (groups.isEmpty()) return List.of();
        return List.of(request(
                alert,
                DETAILS,
                "ASSIST_DETAILS:" + at.toEpochMilli(),
                List.copyOf(groups),
                "Cảnh báo hỗ trợ khẩn cấp vừa có thêm vị trí hoặc người cao tuổi liên quan. Mở cảnh báo để tới ngay.",
                at));
    }

    private static Set<RecipientGroup> floor(long floorAreaId) {
        return Set.of(
                new RecipientGroup.ShiftLead(floorAreaId, new Substitution.Default()),
                new RecipientGroup.OnShiftStaff(floorAreaId, Set.of(), Set.of(), new Substitution.Default()));
    }

    private static NotificationRequest request(
            AlertRecord alert,
            String eventType,
            String suffix,
            List<RecipientGroup> groups,
            String content,
            Instant at) {
        return new NotificationRequest(
                "M05",
                eventType,
                SourceGroup.HEALTH_ALERT,
                new SourceRecord(AlertNotices.SOURCE_TYPE, Long.toString(alert.id())),
                AlertNotices.SOURCE_TYPE + ":" + alert.id() + ":" + suffix,
                Optional.of(NotificationLevel.URGENT),
                Optional.ofNullable(alert.residentId()),
                Optional.ofNullable(alert.floorAreaId()),
                groups,
                List.of(new ContentPart(InformationType.HEALTH, content)),
                Optional.of("Nhân viên cần hỗ trợ khẩn cấp"),
                Optional.of("Cảnh báo Khẩn cấp"),
                Optional.empty(),
                false,
                at,
                Map.of("alertId", alert.id()),
                false,
                Optional.empty(),
                Optional.empty());
    }
}

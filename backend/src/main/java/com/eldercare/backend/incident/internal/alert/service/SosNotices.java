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
import com.eldercare.backend.incident.api.dto.SosTripView;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Người nhận cảnh báo SOS lúc tạo (FR-047e, research S-23): nhóm FR-047a (Điều dưỡng phụ trách, Người phụ trách ca, Bác sĩ
 * trực, Trưởng tầng); không có Bác sĩ trực thì áp FR-047b (mọi Bác sĩ và Quản lý viện); Hoạt động bên ngoài thì thêm trưởng
 * đoàn, người đi cùng. Không có người liên hệ chính.
 */
final class SosNotices {
    static final String EVENT_TYPE = "SOS_ALERT_CREATED";

    private SosNotices() {}

    static List<NotificationRequest> created(
            AlertRecord alert, boolean noOnDutyDoctor, Optional<SosTripView> trip, Instant at) {
        var groups = new LinkedHashSet<RecipientGroup>();
        groups.add(new RecipientGroup.AssignedNurse(alert.residentId(), new Substitution.Default()));
        if (alert.floorAreaId() != null) {
            groups.add(new RecipientGroup.ShiftLead(alert.floorAreaId(), new Substitution.Default()));
            groups.add(new RecipientGroup.FloorManager(alert.floorAreaId(), new Substitution.Default()));
        }
        if (noOnDutyDoctor) {
            groups.add(new RecipientGroup.FacilityRole(Role.DOCTOR));
            groups.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        } else {
            groups.add(new RecipientGroup.OnDutyDoctor(new Substitution.Default()));
        }
        trip.ifPresent(t -> {
            groups.add(new RecipientGroup.TripLeader(t.tripId(), new Substitution.Default()));
            t.companionStaffIds().forEach(id -> groups.add(new RecipientGroup.Staff(id, "SOS_TRIP_COMPANION")));
        });
        return List.of(new NotificationRequest(
                "M05",
                EVENT_TYPE,
                SourceGroup.HEALTH_ALERT,
                new SourceRecord(AlertNotices.SOURCE_TYPE, Long.toString(alert.id())),
                AlertNotices.SOURCE_TYPE + ":" + alert.id() + ":SOS_CREATED",
                Optional.of(NotificationLevel.URGENT),
                Optional.of(alert.residentId()),
                Optional.ofNullable(alert.floorAreaId()),
                List.copyOf(groups),
                List.of(new ContentPart(
                        InformationType.HEALTH,
                        "Người cao tuổi vừa bấm nút SOS. Mở cảnh báo để tiếp nhận và tới ngay.")),
                Optional.of("Người cao tuổi bấm SOS"),
                Optional.of("Cảnh báo Khẩn cấp"),
                Optional.empty(),
                false,
                at,
                Map.of("alertId", alert.id()),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}

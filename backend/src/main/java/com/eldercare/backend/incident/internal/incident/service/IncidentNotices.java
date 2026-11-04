package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Nhóm người nhận khi ghi nhận sự cố (FR-043 a, FR-047 a, FR-047b; research S-19). 009 phân giải nhóm theo phân
 * công hiện tại; sự cố cơ sở vật chất dùng Người phụ trách ca và Trưởng tầng của địa điểm thay Điều dưỡng phụ trách
 * (FR-040).
 */
final class IncidentNotices {
    private static final String SOURCE_TYPE = "M05.INCIDENT";

    private IncidentNotices() {}

    static List<NotificationRequest> recorded(
            long incidentId,
            Long residentId,
            Long floorAreaId,
            IncidentLevel level,
            boolean noOnDutyDoctor,
            long actorId,
            Instant at) {
        var groups = new LinkedHashSet<RecipientGroup>();
        if (level == IncidentLevel.CRITICAL) {
            if (noOnDutyDoctor) {
                groups.add(new RecipientGroup.FacilityRole(Role.DOCTOR));
            } else {
                groups.add(new RecipientGroup.OnDutyDoctor(new Substitution.Default()));
            }
            floorManager(groups, floorAreaId);
            groups.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        }
        if (residentId != null) {
            groups.add(new RecipientGroup.AssignedNurse(residentId, new Substitution.Default()));
        } else {
            shiftLead(groups, floorAreaId);
            floorManager(groups, floorAreaId);
        }
        if (level != IncidentLevel.LOW) shiftLead(groups, floorAreaId);

        String key = SOURCE_TYPE + ":" + incidentId + ":RECORDED";
        var requests = new ArrayList<NotificationRequest>();
        requests.add(request(
                incidentId,
                key,
                level,
                residentId,
                floorAreaId,
                List.copyOf(groups),
                "Có sự cố mới cần tiếp nhận. Mở hồ sơ sự cố để xem chi tiết.",
                actorId,
                at,
                Optional.empty(),
                Optional.empty()));
        if (level == IncidentLevel.CRITICAL && residentId != null) {
            // FR-047 (a): người liên hệ chính chỉ nhận nội dung tối thiểu, không chẩn đoán, chỉ số, thuốc, nguyện vọng.
            requests.add(request(
                    incidentId,
                    key + ":PRIMARY_CONTACT",
                    level,
                    residentId,
                    floorAreaId,
                    List.of(new RecipientGroup.PrimaryContact(residentId)),
                    "Có tình huống khẩn cấp liên quan người thân của ông/bà tại viện. Đề nghị liên hệ viện.",
                    actorId,
                    at,
                    Optional.of(InformationSubject.ABOUT_RESIDENT),
                    Optional.of(Q354Case.PRIMARY_CONTACT_EMERGENCY)));
        }
        return requests;
    }

    private static void shiftLead(LinkedHashSet<RecipientGroup> groups, Long floorAreaId) {
        if (floorAreaId != null) groups.add(new RecipientGroup.ShiftLead(floorAreaId, new Substitution.Default()));
    }

    private static void floorManager(LinkedHashSet<RecipientGroup> groups, Long floorAreaId) {
        if (floorAreaId != null) groups.add(new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default()));
    }

    private static NotificationRequest request(
            long incidentId,
            String key,
            IncidentLevel level,
            Long residentId,
            Long floorAreaId,
            List<RecipientGroup> groups,
            String content,
            long actorId,
            Instant at,
            Optional<InformationSubject> subject,
            Optional<Q354Case> q354Case) {
        return new NotificationRequest(
                "M05",
                "INCIDENT_RECORDED",
                SourceGroup.INCIDENT,
                new SourceRecord(SOURCE_TYPE, Long.toString(incidentId)),
                key,
                Optional.of(notificationLevel(level)),
                Optional.ofNullable(residentId),
                Optional.ofNullable(floorAreaId),
                groups,
                List.of(new ContentPart(InformationType.GENERAL, content)),
                Optional.of("Sự cố mới"),
                Optional.of("Sự cố"),
                Optional.of(actorId),
                false,
                at,
                Map.of("incidentId", incidentId),
                false,
                subject,
                q354Case);
    }

    private static NotificationLevel notificationLevel(IncidentLevel level) {
        return switch (level) {
            case LOW -> NotificationLevel.LOW;
            case MEDIUM -> NotificationLevel.MEDIUM;
            case CRITICAL -> NotificationLevel.URGENT;
        };
    }
}

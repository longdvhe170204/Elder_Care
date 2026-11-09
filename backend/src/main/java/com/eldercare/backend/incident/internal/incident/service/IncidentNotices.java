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
public final class IncidentNotices {
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
        return notices(
                incidentId,
                SOURCE_TYPE + ":" + incidentId + ":RECORDED",
                residentId,
                floorAreaId,
                level,
                noOnDutyDoctor,
                "Có sự cố mới cần tiếp nhận. Mở hồ sơ sự cố để xem chi tiết.",
                actorId,
                at);
    }

    /**
     * FR-047 (a), FR-047b: sự cố được nâng lên Khẩn cấp chạy quy trình khẩn cấp với cùng nhóm người nhận như lúc ghi nhận
     * ở mức Khẩn cấp (T041).
     */
    public static List<NotificationRequest> emergencyActivated(
            long incidentId, long residentId, Long floorAreaId, boolean noOnDutyDoctor, long actorId, Instant at) {
        return notices(
                incidentId,
                SOURCE_TYPE + ":" + incidentId + ":EMERGENCY_ACTIVATED:" + at.toEpochMilli(),
                residentId,
                floorAreaId,
                IncidentLevel.CRITICAL,
                noOnDutyDoctor,
                "Sự cố được nâng lên mức Khẩn cấp. Mở hồ sơ sự cố để xem thẻ thông tin khẩn cấp.",
                actorId,
                at);
    }

    private static List<NotificationRequest> notices(
            long incidentId,
            String key,
            Long residentId,
            Long floorAreaId,
            IncidentLevel level,
            boolean noOnDutyDoctor,
            String staffContent,
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

        var requests = new ArrayList<NotificationRequest>();
        requests.add(request(
                incidentId,
                key,
                level,
                residentId,
                floorAreaId,
                List.copyOf(groups),
                staffContent,
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

    /** FR-051: Người phụ trách ca của tầng lập công việc theo dõi sau ngã (research S-19, mức Trung bình). */
    static List<NotificationRequest> postFallFollowUp(
            long incidentId, long residentId, Long floorAreaId, long actorId, Instant at) {
        var groups = new LinkedHashSet<RecipientGroup>();
        shiftLead(groups, floorAreaId);
        if (groups.isEmpty()) return List.of();
        return List.of(task(
                incidentId,
                "POST_FALL_FOLLOW_UP",
                residentId,
                floorAreaId,
                List.copyOf(groups),
                "Cần lập công việc theo dõi sau ngã trong khoảng theo dõi. Mở hồ sơ sự cố để xem mốc và khoảng đo.",
                actorId,
                at));
    }

    /** FR-060: Điều dưỡng phụ trách và các Bác sĩ lập danh sách tiếp xúc (research S-15, S-19, mức Trung bình). */
    static List<NotificationRequest> contactListRequested(
            long incidentId, long residentId, Long floorAreaId, long actorId, Instant at) {
        return List.of(task(
                incidentId,
                "CONTACT_LIST_REQUESTED",
                residentId,
                floorAreaId,
                List.of(
                        new RecipientGroup.AssignedNurse(residentId, new Substitution.Default()),
                        new RecipientGroup.FacilityRole(Role.DOCTOR)),
                "Cần lập danh sách tiếp xúc cho sự cố. Mở hồ sơ sự cố để tra cứu dữ liệu.",
                actorId,
                at));
    }

    /** FR-046a: tác động còn hiệu lực sau Hủy ghi nhận, đổi loại; Điều dưỡng phụ trách và các Bác sĩ tự quyết. */
    static List<NotificationRequest> effectiveImpacts(
            long incidentId, Long residentId, Long floorAreaId, long correctionId, List<String> impacts, Instant at) {
        var groups = new ArrayList<RecipientGroup>();
        if (residentId != null) groups.add(new RecipientGroup.AssignedNurse(residentId, new Substitution.Default()));
        groups.add(new RecipientGroup.FacilityRole(Role.DOCTOR));
        return List.of(new NotificationRequest(
                "M05",
                "EFFECTIVE_IMPACTS_REMAIN",
                SourceGroup.INCIDENT,
                new SourceRecord(SOURCE_TYPE, Long.toString(incidentId)),
                SOURCE_TYPE + ":" + incidentId + ":EFFECTIVE_IMPACTS_REMAIN:" + correctionId,
                Optional.of(NotificationLevel.LOW),
                Optional.ofNullable(residentId),
                Optional.ofNullable(floorAreaId),
                groups,
                List.of(new ContentPart(
                        InformationType.HEALTH,
                        "Sự cố đã được đính chính; các tác động sau vẫn còn hiệu lực, cần xem xét: "
                                + String.join(", ", impacts))),
                Optional.of("Có việc cần xem xét"),
                Optional.of("Sự cố"),
                Optional.empty(),
                true,
                at,
                Map.of("incidentId", incidentId, "impacts", impacts),
                false,
                Optional.empty(),
                Optional.empty()));
    }

    private static NotificationRequest task(
            long incidentId,
            String eventType,
            long residentId,
            Long floorAreaId,
            List<RecipientGroup> groups,
            String content,
            long actorId,
            Instant at) {
        return new NotificationRequest(
                "M05",
                eventType,
                SourceGroup.INCIDENT,
                new SourceRecord(SOURCE_TYPE, Long.toString(incidentId)),
                SOURCE_TYPE + ":" + incidentId + ":" + eventType,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(residentId),
                Optional.ofNullable(floorAreaId),
                groups,
                List.of(new ContentPart(InformationType.HEALTH, content)),
                Optional.of("Có việc cần xử lý"),
                Optional.of("Sự cố"),
                Optional.of(actorId),
                false,
                at,
                Map.of("incidentId", incidentId),
                false,
                Optional.empty(),
                Optional.empty());
    }

    /** FR-045a: báo người được giao theo mức hiện hành của sự cố. */
    /** Nguồn cảnh báo của sự cố đã được xử lý: báo người xử lý, mức Nhẹ (FR-032, FR-085). */
    public static List<NotificationRequest> sourceResolved(
            long incidentId, Long residentId, Long floorAreaId, long staffId, Instant at) {
        return List.of(request(
                incidentId,
                SOURCE_TYPE + ":" + incidentId + ":SOURCE_RESOLVED:" + at.toEpochMilli(),
                IncidentLevel.LOW,
                residentId,
                floorAreaId,
                List.of(new RecipientGroup.Staff(staffId, "INCIDENT_SOURCE_RESOLVED")),
                "Nguồn của cảnh báo đã chuyển thành sự cố này đã được xử lý. Mở hồ sơ sự cố để xem diễn biến.",
                0L,
                at,
                Optional.empty(),
                Optional.empty()));
    }

    static List<NotificationRequest> assigned(
            long incidentId,
            Long residentId,
            Long floorAreaId,
            IncidentLevel level,
            long staffId,
            long actorId,
            Instant at) {
        return List.of(request(
                incidentId,
                SOURCE_TYPE + ":" + incidentId + ":ASSIGNED:" + staffId + ":" + at.toEpochMilli(),
                level,
                residentId,
                floorAreaId,
                List.of(new RecipientGroup.Staff(staffId, "INCIDENT_ASSIGNED")),
                "Bạn được giao xử lý một sự cố. Mở hồ sơ sự cố để xem ghi chú của người giao.",
                actorId,
                at,
                Optional.empty(),
                Optional.empty()));
    }

    /**
     * FR-044: sự cố quá hạn tiếp nhận lên cấp theo chuỗi của cảnh báo (FR-033): cấp 1 Trưởng tầng, Bác sĩ trực; cấp 2
     * Quản lý viện; "không thay" để 009 bỏ qua cấp không có người (Q-110). Mức Trung bình như cảnh báo leo thang.
     */
    static List<NotificationRequest> escalated(
            long incidentId, Long residentId, Long floorAreaId, int level, Instant at) {
        var groups = new ArrayList<RecipientGroup>();
        if (level == 1) {
            if (floorAreaId != null) {
                groups.add(new RecipientGroup.FloorManager(floorAreaId, new Substitution.NoSubstitute()));
            }
            groups.add(new RecipientGroup.OnDutyDoctor(new Substitution.NoSubstitute()));
        } else {
            groups.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        }
        return List.of(escalation(
                incidentId,
                "INCIDENT_ESCALATED",
                SOURCE_TYPE + ":" + incidentId + ":ESCALATED:" + level,
                residentId,
                floorAreaId,
                groups,
                "Sự cố quá hạn tiếp nhận và đã leo thang lên cấp bạn phụ trách. Mở danh sách sự cố để tiếp nhận.",
                at));
    }

    /** FR-044 theo FR-034: nhắc sự cố đã leo thang tối đa tới Quản lý viện và Trưởng tầng (Q-248, Q-266). */
    static List<NotificationRequest> maximumReminder(
            long incidentId, Long residentId, Long floorAreaId, int sequence, Instant at) {
        var groups = new LinkedHashSet<RecipientGroup>();
        groups.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        floorManager(groups, floorAreaId);
        return List.of(escalation(
                incidentId,
                "INCIDENT_MAXIMUM_ESCALATION_REMINDER",
                SOURCE_TYPE + ":" + incidentId + ":MAX_REMINDER:" + sequence,
                residentId,
                floorAreaId,
                List.copyOf(groups),
                "Sự cố đã leo thang tối đa mà vẫn chưa có người tiếp nhận. Mở danh sách sự cố để xử lý.",
                at));
    }

    private static NotificationRequest escalation(
            long incidentId,
            String eventType,
            String key,
            Long residentId,
            Long floorAreaId,
            List<RecipientGroup> groups,
            String content,
            Instant at) {
        return new NotificationRequest(
                "M05",
                eventType,
                SourceGroup.INCIDENT,
                new SourceRecord(SOURCE_TYPE, Long.toString(incidentId)),
                key,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.ofNullable(residentId),
                Optional.ofNullable(floorAreaId),
                groups,
                List.of(new ContentPart(InformationType.HEALTH, content)),
                Optional.of("Sự cố cần tiếp nhận"),
                Optional.of("Sự cố"),
                Optional.empty(),
                false,
                at,
                Map.of("incidentId", incidentId),
                false,
                Optional.empty(),
                Optional.empty());
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
                actorId > 0 ? Optional.of(actorId) : Optional.empty(),
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

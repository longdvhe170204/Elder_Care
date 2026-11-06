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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yêu cầu thông báo của vòng đời cảnh báo (FR-033, FR-034, FR-038a; research S-19). Nhóm người nhận của cấp leo thang
 * khai "không thay" để 009 bỏ qua cấp không có người (Q-110).
 */
final class AlertNotices {
    static final String SOURCE_TYPE = "M05.ALERT";

    private AlertNotices() {}

    /** Người nhận của cấp 1 (Trưởng tầng, Bác sĩ trực) và cấp 2 (Quản lý viện). */
    static List<RecipientGroup> levelGroups(int level, Long floorAreaId) {
        var groups = new ArrayList<RecipientGroup>();
        if (level == 1) {
            if (floorAreaId != null) {
                groups.add(new RecipientGroup.FloorManager(floorAreaId, new Substitution.NoSubstitute()));
            }
            groups.add(new RecipientGroup.OnDutyDoctor(new Substitution.NoSubstitute()));
        } else {
            groups.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        }
        return groups;
    }

    static List<NotificationRequest> escalated(AlertRecord alert, int level, Instant at) {
        return List.of(request(
                alert,
                "ALERT_ESCALATED",
                SOURCE_TYPE + ":" + alert.id() + ":ESCALATED:" + level,
                levelGroups(level, alert.floorAreaId()),
                "Cảnh báo quá hạn tiếp nhận và đã leo thang lên cấp bạn phụ trách. Mở danh sách cảnh báo để tiếp nhận.",
                at));
    }

    /** Nhắc cảnh báo đã leo thang tối đa: Quản lý viện và Trưởng tầng của tầng (FR-034, Q-248, Q-266). */
    static List<NotificationRequest> maximumReminder(AlertRecord alert, int sequence, Instant at) {
        var groups = new ArrayList<RecipientGroup>();
        groups.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        if (alert.floorAreaId() != null) {
            groups.add(new RecipientGroup.FloorManager(alert.floorAreaId(), new Substitution.Default()));
        }
        return List.of(request(
                alert,
                "ALERT_MAXIMUM_ESCALATION_REMINDER",
                SOURCE_TYPE + ":" + alert.id() + ":MAX_REMINDER:" + sequence,
                groups,
                "Cảnh báo đã leo thang tối đa mà vẫn chưa có người tiếp nhận. Mở danh sách cảnh báo để xử lý.",
                at));
    }

    /** Báo lặp lại: người phụ trách và Bác sĩ trực (FR-038a); gửi lại ở mỗi lần phát sinh khi điều kiện còn đúng. */
    static List<NotificationRequest> repeated(AlertRecord alert, int count, String sourceKey, Instant at) {
        var groups = new ArrayList<RecipientGroup>();
        if (alert.residentId() != null) {
            groups.add(new RecipientGroup.AssignedNurse(alert.residentId(), new Substitution.Default()));
        }
        groups.add(new RecipientGroup.OnDutyDoctor(new Substitution.Default()));
        return List.of(request(
                alert,
                "ALERT_REPEATED",
                SOURCE_TYPE + ":" + alert.id() + ":REPEATED:" + sourceKey,
                groups,
                "Cảnh báo cùng loại đã lặp " + count + " lần trong 24 giờ. Người có quyền xem xét nâng mức.",
                at));
    }

    /** Chỉ số nguồn bị đính chính: báo người phụ trách (hoặc điều dưỡng phụ trách), không đóng cảnh báo (FR-019). */
    static List<NotificationRequest> sourceCorrected(AlertRecord alert, String eventKey, Instant at) {
        var groups = new ArrayList<RecipientGroup>();
        if (alert.assigneeId() != null) {
            groups.add(new RecipientGroup.Staff(alert.assigneeId(), "ALERT_SOURCE_CORRECTED"));
        } else if (alert.residentId() != null) {
            groups.add(new RecipientGroup.AssignedNurse(alert.residentId(), new Substitution.Default()));
        }
        return List.of(request(
                alert,
                "ALERT_SOURCE_CORRECTED",
                SOURCE_TYPE + ":" + alert.id() + ":SOURCE_CORRECTED:" + eventKey,
                groups,
                "Chỉ số làm căn cứ của cảnh báo đã được đính chính. Cảnh báo chưa tự đóng, hãy xem xét lại.",
                at,
                NotificationLevel.LOW));
    }

    /** Báo người được giao phụ trách (bảng FR-029 "Giao người phụ trách"). */
    static List<NotificationRequest> assigned(AlertRecord alert, long staffId, Instant at) {
        return List.of(request(
                alert,
                "ALERT_ASSIGNED",
                SOURCE_TYPE + ":" + alert.id() + ":ASSIGNED:" + staffId + ":" + at.toEpochMilli(),
                List.of(new RecipientGroup.Staff(staffId, "ALERT_ASSIGNED")),
                "Bạn được giao phụ trách một cảnh báo. Mở cảnh báo để xem ghi chú của người giao.",
                at,
                NotificationLevel.LOW));
    }

    private static NotificationRequest request(
            AlertRecord alert, String eventType, String key, List<RecipientGroup> groups, String content, Instant at) {
        return request(alert, eventType, key, groups, content, at, NotificationLevel.MEDIUM);
    }

    /** Mức theo FR-085 (Q-311) cho thông báo spec không nêu mức. */
    private static NotificationRequest request(
            AlertRecord alert,
            String eventType,
            String key,
            List<RecipientGroup> groups,
            String content,
            Instant at,
            NotificationLevel level) {
        return new NotificationRequest(
                "M05",
                eventType,
                SourceGroup.HEALTH_ALERT,
                new SourceRecord(SOURCE_TYPE, Long.toString(alert.id())),
                key,
                Optional.of(level),
                Optional.ofNullable(alert.residentId()),
                Optional.ofNullable(alert.floorAreaId()),
                groups,
                List.of(new ContentPart(InformationType.HEALTH, content)),
                Optional.of("Cảnh báo cần xử lý"),
                Optional.of("Cảnh báo"),
                Optional.empty(),
                false,
                at,
                Map.of("alertId", alert.id()),
                false,
                Optional.empty(),
                Optional.empty());
    }
}

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Nhóm người nhận của cảnh báo Khẩn cấp khi tạo hoặc nâng mức (FR-047a): Điều dưỡng phụ trách, Người phụ trách ca,
     * Trưởng tầng, Bác sĩ trực; không có Bác sĩ trực thì mọi Bác sĩ và Quản lý viện (FR-047b). Không có người liên hệ chính.
     */
    static Set<RecipientGroup> emergencyGroups(AlertRecord alert, boolean noOnDutyDoctor) {
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
        return groups;
    }

    /**
     * Thông báo khi cảnh báo được tạo hoặc nâng lên mức hiện tại (FR-035): Nhẹ — người phụ trách ban đầu; Trung bình — cộng
     * nhân viên thực hiện công việc nguồn khi nguồn cho biết; Khẩn cấp — {@link #emergencyGroups}. Cảnh báo chưa gắn người
     * cao tuổi (nút hỗ trợ nhân viên) có đường thông báo riêng nên không dựng ở đây. {@code raised} đổi khóa sự kiện.
     */
    static List<NotificationRequest> created(AlertRecord alert, Long sourceStaffId, boolean raised, Instant at) {
        if (alert.residentId() == null || alert.level() == null) return List.of();
        var groups = new LinkedHashSet<RecipientGroup>();
        NotificationLevel level;
        switch (alert.level()) {
            case CRITICAL -> {
                groups.addAll(emergencyGroups(alert, alert.noOnDutyDoctor()));
                level = NotificationLevel.URGENT;
            }
            case MEDIUM -> {
                groups.add(new RecipientGroup.AssignedNurse(alert.residentId(), new Substitution.Default()));
                if (sourceStaffId != null) {
                    groups.add(new RecipientGroup.Staff(sourceStaffId, "ALERT_SOURCE_PERFORMER"));
                }
                level = NotificationLevel.MEDIUM;
            }
            default -> {
                groups.add(new RecipientGroup.AssignedNurse(alert.residentId(), new Substitution.Default()));
                level = NotificationLevel.LOW;
            }
        }
        return List.of(request(
                alert,
                raised ? "ALERT_LEVEL_RAISED" : "ALERT_CREATED",
                SOURCE_TYPE + ":" + alert.id() + (raised ? ":RAISED:" + alert.level() : ":CREATED"),
                List.copyOf(groups),
                raised
                        ? "Cảnh báo đã được nâng mức. Mở cảnh báo để xử lý."
                        : "Có cảnh báo mới cần xử lý. Mở cảnh báo để tiếp nhận.",
                at,
                level));
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

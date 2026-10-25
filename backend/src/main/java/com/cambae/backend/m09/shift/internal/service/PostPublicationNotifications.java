package com.cambae.backend.m09.shift.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.*;
import com.cambae.backend.m09.assignment.internal.service.AssignmentSources;
import com.cambae.backend.m09.shift.internal.domain.WorkShift;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

/** FR-053/058/059: tham số chỉ có ca/phạm vi; không nhận lý do để tránh lộ qua kênh ngoài. */
@Service
public class PostPublicationNotifications {
    private final AssignmentSources sources;

    public PostPublicationNotifications(AssignmentSources sources) {
        this.sources = sources;
    }

    public List<NotificationRequest> request(
            String event, WorkShift s, Collection<Long> staff, Instant at, boolean missing) {
        return request(event, s, staff, at, missing, new SourceRecord("M09.SHIFT", Long.toString(s.id())));
    }

    public List<NotificationRequest> handover(WorkShift s, long handover, long staff, Instant at) {
        return request(
                "HANDOVER_REASSIGNED",
                s,
                List.of(staff),
                at,
                false,
                new SourceRecord("M09.HANDOVER", Long.toString(handover)));
    }

    private List<NotificationRequest> request(
            String event, WorkShift s, Collection<Long> staff, Instant at, boolean missing, SourceRecord source) {
        var groups = new ArrayList<RecipientGroup>();
        staff.stream().distinct().sorted().forEach(id -> groups.add(new RecipientGroup.Staff(id, event)));
        Collection<Long> floors = "FLOOR".equals(s.scopeType())
                ? List.of(s.scopeId())
                : "AREA".equals(s.scopeType()) ? sources.structure().floorsInArea(s.scopeId()) : List.of();
        floors.forEach(f -> groups.add(new RecipientGroup.FloorManager(f, new Substitution.Default())));
        if (missing || floors.isEmpty()) groups.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        String title = switch (event) {
            case "STAFF_ABSENT" -> "Đã ghi nhận vắng ca; cần kiểm tra phân công và công việc chung";
            case "STAFF_ADDED" -> "Đã bổ sung nhân viên vào ca";
            case "LEAD_CHANGED" -> "Đã chuyển Người phụ trách ca";
            case "CANCELLED" -> "Ca đã được hủy";
            case "SHIFT_SWAP_015" -> "Ca đã thay đổi theo yêu cầu";
            case "HANDOVER_REASSIGNED" -> "Bạn tiếp nhận bàn giao đang dở";
            default -> "Ca thiếu Người phụ trách; cần chỉ định người thay";
        };
        String scope = "FLOOR".equals(s.scopeType())
                ? "tầng #" + s.scopeId()
                : "AREA".equals(s.scopeType()) ? "khu vực #" + s.scopeId() : "toàn viện";
        String message = "Ca #" + s.id() + ", " + scope + ": " + title + ". Mở ứng dụng để kiểm tra.";
        return List.of(new NotificationRequest(
                "008",
                event,
                SourceGroup.SCHEDULE_CHANGE,
                source,
                event + ":" + source.id() + ":" + at,
                Optional.of(
                        Set.of("STAFF_ADDED", "LEAD_CHANGED", "CANCELLED", "SHIFT_SWAP_015", "HANDOVER_REASSIGNED")
                                        .contains(event)
                                ? NotificationLevel.LOW
                                : NotificationLevel.MEDIUM),
                Optional.empty(),
                Optional.ofNullable(s.scopeId()),
                groups,
                List.of(new ContentPart(InformationType.GENERAL, message)),
                Optional.of(message),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of("shiftId", s.id()),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}

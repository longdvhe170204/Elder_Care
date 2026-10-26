package com.cambae.backend.m09.staff.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** FR-053/058: khuôn chỉ có thông tin chung; không mang lý do nhân sự hoặc số/scan chứng chỉ. */
@Service
public class StaffNotifications {
    public List<NotificationRequest> employment(long id, Collection<Long> floors, Instant at) {
        return request(
                "STAFF_EMPLOYMENT_ENDED",
                "M09.STAFF",
                id,
                NotificationLevel.MEDIUM,
                null,
                floors,
                false,
                "Nhân sự trong phạm vi ca đã thay đổi",
                at);
    }

    public List<NotificationRequest> missingLeadInFloors(long shift, Collection<Long> floors, Instant at) {
        return request(
                "SHIFT_LEAD_MISSING",
                "M09.SHIFT",
                shift,
                NotificationLevel.MEDIUM,
                null,
                floors,
                true,
                "Ca thiếu Người phụ trách, cần chỉ định thay",
                at);
    }

    public List<NotificationRequest> training(
            long id,
            long staff,
            String staffName,
            String name,
            LocalDate expiry,
            Collection<Long> floors,
            Collection<Long> shiftIds,
            Instant at) {
        return request(
                "CREDENTIAL_EXPIRING",
                "M09.CREDENTIAL",
                id,
                NotificationLevel.LOW,
                staff,
                floors,
                true,
                name + " sắp hết hạn ngày " + expiry.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu")),
                "Nhân viên " + staffName + ": " + name + ", hết hạn "
                        + expiry.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"))
                        + (shiftIds.isEmpty() ? "" : ". Ca bị ảnh hưởng: " + references(shiftIds)),
                at);
    }

    public List<NotificationRequest> invalidated(
            long id, Collection<Long> floors, Collection<Long> shiftIds, Collection<Long> residents, Instant at) {
        return request(
                "ASSIGNMENTS_ENDED_BY_CREDENTIAL",
                "M09.STAFF",
                id,
                NotificationLevel.MEDIUM,
                null,
                floors,
                false,
                "Phân công đã kết thúc do chứng chỉ hết hiệu lực; cần phân công thay",
                "Nhân viên #" + id + ": phân công đã kết thúc do chứng chỉ hết hiệu lực. Ca: " + references(shiftIds)
                        + ". Người cao tuổi: " + references(residents),
                at);
    }

    public List<NotificationRequest> missingDepartment(long id, Instant at) {
        return request(
                "DEPARTMENT_LEAD_MISSING",
                "M09.DEPARTMENT",
                id,
                NotificationLevel.MEDIUM,
                null,
                List.of(),
                true,
                "Bộ phận chưa có Phụ trách, cần chỉ định",
                at);
    }

    private String references(Collection<Long> ids) {
        return ids.stream().sorted().map(id -> "#" + id).collect(java.util.stream.Collectors.joining(", "));
    }

    private List<NotificationRequest> request(
            String event,
            String type,
            long id,
            NotificationLevel level,
            Long person,
            Collection<Long> floors,
            boolean manager,
            String title,
            Instant at) {
        return request(event, type, id, level, person, floors, manager, title, title, at);
    }

    private List<NotificationRequest> request(
            String event,
            String type,
            long id,
            NotificationLevel level,
            Long person,
            Collection<Long> floors,
            boolean manager,
            String title,
            String content,
            Instant at) {
        var groups = new ArrayList<RecipientGroup>();
        if (person != null) groups.add(new RecipientGroup.Staff(person, event));
        floors.stream()
                .distinct()
                .sorted()
                .forEach(f -> groups.add(new RecipientGroup.FloorManager(f, new Substitution.Default())));
        if (manager) groups.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        if (groups.isEmpty()) return List.of();
        return List.of(new NotificationRequest(
                "008",
                event,
                SourceGroup.OTHER,
                new SourceRecord(type, Long.toString(id)),
                event + ":" + id + ":" + at,
                Optional.of(level),
                Optional.empty(),
                Optional.empty(),
                groups,
                List.of(new ContentPart(InformationType.GENERAL, content)),
                Optional.of(title),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}

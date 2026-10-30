package com.cambae.backend.m09.assignment.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.api.events.*;
import com.cambae.backend.m09.internal.service.M09TimedTask;
import com.cambae.backend.m09.shift.internal.domain.WorkShift;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** FR-024/029: hai mốc nhắc từ lệnh công bố và chuyển trạng thái; không tạo phân công. */
@Service
@Order(30)
public class AssignmentReminders implements M09TimedTask {
    private final AssignmentQueries assignments;
    private final AssignmentSources sources;
    private final ShiftStore shifts;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final JdbcTemplate jdbc;

    public AssignmentReminders(
            AssignmentQueries assignments,
            AssignmentSources sources,
            ShiftStore shifts,
            BusinessTime time,
            ApplicationEventPublisher events,
            JdbcTemplate jdbc) {
        this.assignments = assignments;
        this.sources = sources;
        this.shifts = shifts;
        this.time = time;
        this.events = events;
        this.jdbc = jdbc;
    }

    public void published(long schedule, Instant at) {
        for (var shift : shifts.scheduleShifts(schedule)) missingResidents(shift, "PUBLICATION", at);
    }

    @EventListener
    public void started(ShiftStarted event) {
        var shift = shifts.shift(event.shiftId(), false);
        missingResidents(shift, "START", event.occurredAt());
        if (shift.scopeId() != null
                && assignments.floorManager(shift.scopeId(), event.occurredAt()).isEmpty())
            missingFloorManager(
                    shift.scopeId(),
                    event.occurredAt(),
                    new SourceRecord("M09.SHIFT", Long.toString(shift.id())),
                    "SHIFT:" + shift.id());
    }

    public void missingFloorManager(long floor, Instant at, SourceRecord source, String basis) {
        var request = request(
                "FLOOR_MANAGER_MISSING",
                source,
                "FLOOR_MANAGER_MISSING:" + floor + ":" + basis,
                floor,
                at,
                "Tầng chưa có Trưởng tầng được giao",
                List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                Map.of("floorAreaId", floor));
        events.publishEvent(new FloorManagerMissing(floor, at, List.of(request)));
    }

    private void missingResidents(WorkShift shift, String stage, Instant at) {
        if (!sources.notificationSourcesAvailable()) return; // Giữ cờ chưa xử lý để thử lại khi nguồn B/C có bản cài.
        var missing =
                sources
                        .residents(shift.scopeType(), shift.scopeId() == null ? 0 : shift.scopeId(), shift.startsAt())
                        .stream()
                        .filter(id -> !sources.terminal(id))
                        .filter(id ->
                                assignments.assignedNurses(id, shift.startsAt()).isEmpty()
                                        || assignments
                                                .primary(id, Role.CAREGIVER, null, shift.startsAt(), false)
                                                .isEmpty())
                        .sorted()
                        .toList();
        String column =
                "START".equals(stage) ? "unassigned_residents_notified_at" : "publication_assignments_notified_at";
        if (jdbc.update(
                        "UPDATE work_shift SET " + column + "=? WHERE id=? AND " + column + " IS NULL",
                        ShiftStore.sql(time.now()),
                        shift.id())
                != 1) return;
        if (missing.isEmpty()) return;
        var recipients = new ArrayList<RecipientGroup>();
        if (shift.scopeId() != null)
            recipients.add(new RecipientGroup.FloorManager(shift.scopeId(), new Substitution.Default()));
        if (shift.leadStaffId() != null) recipients.add(new RecipientGroup.Staff(shift.leadStaffId(), "SHIFT_LEAD"));
        if (recipients.isEmpty()) recipients.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        var request = request(
                "RESIDENTS_WITHOUT_ASSIGNED_STAFF",
                new SourceRecord("M09.SHIFT", Long.toString(shift.id())),
                "RESIDENTS_WITHOUT_ASSIGNED_STAFF:" + shift.id() + ":" + stage,
                shift.scopeId(),
                at,
                "Người cao tuổi chưa có đủ nhân viên phụ trách",
                recipients,
                Map.of("shiftId", shift.id(), "residentIds", missing, "stage", stage));
        events.publishEvent(new ResidentsWithoutAssignedStaff(shift.id(), missing, at, List.of(request)));
    }

    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    public void run(Instant scheduled, Instant at) {
        if (!sources.notificationSourcesAvailable()) return;
        for (long id : jdbc.queryForList(
                "SELECT id FROM work_shift WHERE status NOT IN ('DRAFT','CANCELLED') AND (publication_assignments_notified_at IS NULL OR (starts_at<=? AND unassigned_residents_notified_at IS NULL)) ORDER BY id",
                Long.class,
                ShiftStore.sql(at))) {
            var shift = shifts.shift(id, false);
            missingResidents(shift, "PUBLICATION", at);
            if (!shift.startsAt().isAfter(at)) missingResidents(shift, "START", at);
        }
    }

    private NotificationRequest request(
            String event,
            SourceRecord source,
            String key,
            Long floor,
            Instant at,
            String title,
            List<RecipientGroup> recipients,
            Map<String, Object> details) {
        return new NotificationRequest(
                "008",
                event,
                SourceGroup.SCHEDULE_CHANGE,
                source,
                key,
                Optional.of(NotificationLevel.LOW),
                Optional.empty(),
                Optional.ofNullable(floor),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, title)),
                Optional.of(title),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                details,
                false,
                Optional.empty(),
                Optional.empty());
    }
}

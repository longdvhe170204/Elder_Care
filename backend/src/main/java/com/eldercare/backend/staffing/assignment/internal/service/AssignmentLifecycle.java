package com.eldercare.backend.staffing.assignment.internal.service;

import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.staffing.assignment.internal.persistence.AssignmentStore;
import com.eldercare.backend.staffing.internal.service.M09TimedTask;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.staffing.staff.internal.persistence.StaffStore;
import java.time.Instant;
import java.util.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-027: chỉ chuyển trạng thái lệnh đã có; khóa cùng thứ tự với lệnh người dùng trước vòng đời ca. */
@Service
@Order(10)
public class AssignmentLifecycle implements M09TimedTask {
    private final AssignmentStore store;
    private final StaffStore staff;
    private final ShiftStore shifts;
    private final AuditLogApi audit;
    private final AssignmentQueries queries;
    private final AssignmentReminders reminders;

    public AssignmentLifecycle(
            AssignmentStore store,
            StaffStore staff,
            ShiftStore shifts,
            AuditLogApi audit,
            AssignmentQueries queries,
            AssignmentReminders reminders) {
        this.store = store;
        this.staff = staff;
        this.shifts = shifts;
        this.audit = audit;
        this.queries = queries;
        this.reminders = reminders;
    }

    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    @Transactional
    public void run(Instant scheduled, Instant at) {
        var due = store.due(at);
        var expired = store.expiringManagers(at);
        java.util.stream.Stream.concat(
                        due.stream().map(a -> a.staffId()), expired.stream().map(a -> a.staffId()))
                .distinct()
                .sorted()
                .forEach(staff::lock);
        var parents = due.stream()
                .map(a -> shifts.shift(a.shiftId(), false))
                .distinct()
                .toList();
        var floors = new TreeSet<Long>();
        parents.stream().map(s -> s.scopeId()).filter(Objects::nonNull).forEach(floors::add);
        expired.forEach(a -> floors.add(a.floorAreaId()));
        store.lockFloors(floors);
        parents.stream().map(s -> s.scheduleId()).distinct().sorted().forEach(id -> shifts.schedule(id, true));
        parents.stream().map(s -> s.id()).distinct().sorted().forEach(id -> shifts.shift(id, true));
        for (var candidate : due) {
            var row = store.find(candidate.id(), true);
            if (!Set.of("SCHEDULED", "ACTIVE").contains(row.status())) continue;
            String status = !row.endsAt().isAfter(at) ? "ENDED" : "ACTIVE";
            if (!row.status().equals(status)) {
                store.transition(row, status, null, null);
                audit.write(AuditLogEntry.builder()
                        .action(new AuditAction("ASSIGNMENT_STATUS_CHANGED", false))
                        .entity("M09.ASSIGNMENT", row.id())
                        .before(Map.of("status", row.status()))
                        .after(Map.of("status", status))
                        .basis(new Basis("FR-027", null, row.shiftId(), null))
                        .details(Map.of("trigger", "TIME"))
                        .build());
            }
        }
        for (var candidate : expired) {
            var row = store.manager(candidate.id(), true);
            if (!"ACTIVE".equals(row.status())) continue;
            store.shorten(row, row.endsAt(), true, row.reason());
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("FLOOR_MANAGER_ASSIGNMENT_STATUS_CHANGED", false))
                    .entity("M09.FLOOR_MANAGER_ASSIGNMENT", row.id())
                    .before(Map.of("status", "ACTIVE"))
                    .after(Map.of("status", "ENDED"))
                    .basis(new Basis("FR-024", null, row.id(), null))
                    .details(Map.of("trigger", "TIME"))
                    .build());
            if (queries.floorManager(row.floorAreaId(), at).isEmpty())
                reminders.missingFloorManager(
                        row.floorAreaId(),
                        at,
                        new SourceRecord("M09.FLOOR_MANAGER_ASSIGNMENT", Long.toString(row.id())),
                        "ASSIGNMENT:" + row.id());
        }
    }
}

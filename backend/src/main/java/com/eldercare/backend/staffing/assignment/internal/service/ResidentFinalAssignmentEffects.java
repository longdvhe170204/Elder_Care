package com.eldercare.backend.staffing.assignment.internal.service;

import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import com.eldercare.backend.staffing.api.events.AssignmentChanged;
import com.eldercare.backend.staffing.assignment.internal.persistence.AssignmentStore;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.staffing.staff.internal.persistence.StaffStore;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** N-08: tác động trong transaction lệnh đổi trạng thái của 001, không phải lệnh người dùng độc lập. */
@Service
public class ResidentFinalAssignmentEffects {
    private final AssignmentStore store;
    private final ShiftStore shifts;
    private final StaffStore staff;
    private final AssignmentSources sources;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;

    public ResidentFinalAssignmentEffects(
            AssignmentStore store,
            ShiftStore shifts,
            StaffStore staff,
            AssignmentSources sources,
            AuditLogApi audit,
            ApplicationEventPublisher events) {
        this.store = store;
        this.shifts = shifts;
        this.staff = staff;
        this.sources = sources;
        this.audit = audit;
        this.events = events;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void changed(ResidentStatusChanged event) {
        var initial = store.directResident(event.residentId(), false);
        // Không có phân công trực tiếp thì M09 không có tác động, không cần đọc nguồn hồ sơ.
        if (initial.isEmpty()) return;
        if (!sources.terminal(event.residentId())) return;
        initial.stream().map(a -> a.staffId()).distinct().sorted().forEach(staff::lock);
        var parents = initial.stream()
                .map(a -> shifts.shift(a.shiftId(), false))
                .distinct()
                .toList();
        store.lockFloors(
                parents.stream().map(s -> s.scopeId()).filter(Objects::nonNull).toList());
        parents.stream().map(s -> s.scheduleId()).distinct().sorted().forEach(id -> shifts.schedule(id, true));
        parents.stream().map(s -> s.id()).distinct().sorted().forEach(id -> shifts.shift(id, true));
        var actual = store.directResident(event.residentId(), true);
        if (!initial.stream()
                .map(a -> a.id())
                .sorted()
                .toList()
                .equals(actual.stream().map(a -> a.id()).sorted().toList()))
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "STATE_CHANGED",
                    "Phân công đã thay đổi trong lúc chuyển trạng thái; hãy thực hiện lại lệnh");
        for (var a : actual) {
            boolean future = a.startsAt().isAfter(event.at());
            String status = future ? "CANCELLED" : "ENDED";
            Instant cut = future ? null : a.endsAt().isBefore(event.at()) ? a.endsAt() : event.at();
            if (!future && "SCHEDULED".equals(a.status())) store.transition(a, "ACTIVE", null, null);
            store.transition(a, status, cut, "Người cao tuổi chuyển trạng thái cuối");
            if (!future && a.endsAt().isAfter(event.at())) sources.ended(a.id(), a.shiftId());
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("ASSIGNMENT_ENDED_BY_RESIDENT_STATUS", false))
                    .entity("M09.ASSIGNMENT", a.id())
                    .before(Map.of("status", a.status()))
                    .after(Map.of("status", status))
                    .basis(new Basis("FR-026", null, event.historyId(), null))
                    .details(Map.of(
                            "trigger",
                            "RESIDENT_FINAL_STATUS",
                            "residentId",
                            event.residentId(),
                            "sourceHistoryId",
                            event.historyId()))
                    .build());
            events.publishEvent(new AssignmentChanged(a.shiftId(), a.staffId(), event.at()));
        }
    }
}

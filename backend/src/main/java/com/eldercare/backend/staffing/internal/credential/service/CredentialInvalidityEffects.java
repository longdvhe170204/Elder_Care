package com.eldercare.backend.staffing.internal.credential.service;

import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.events.*;
import com.eldercare.backend.staffing.internal.assignment.persistence.AssignmentStore;
import com.eldercare.backend.staffing.internal.assignment.service.AssignmentSources;
import com.eldercare.backend.staffing.internal.credential.domain.*;
import com.eldercare.backend.staffing.internal.credential.persistence.CredentialStore;
import com.eldercare.backend.staffing.internal.service.M09TimedTask;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.staff.persistence.StaffStore;
import com.eldercare.backend.staffing.internal.staff.service.StaffNotifications;
import java.time.*;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T034/FR-009: kết thúc đúng mốc, chỉ các nhiệm vụ cần loại chứng chỉ và chưa có bản hợp lệ thay. */
@Service
@org.springframework.core.annotation.Order(10)
public class CredentialInvalidityEffects implements M09TimedTask {
    private final CredentialStore credentials;
    private final StaffStore staff;
    private final AssignmentStore assignments;
    private final ShiftStore shifts;
    private final AssignmentSources sources;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final StaffNotifications notifications;

    public CredentialInvalidityEffects(
            CredentialStore credentials,
            StaffStore staff,
            AssignmentStore assignments,
            ShiftStore shifts,
            AssignmentSources sources,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            StaffNotifications notifications) {
        this.credentials = credentials;
        this.staff = staff;
        this.assignments = assignments;
        this.shifts = shifts;
        this.sources = sources;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.notifications = notifications;
    }

    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    public Instant invalidAt(Credential c) {
        Instant expiry =
                c.expiryDate() == null ? null : time.startOfDay(c.expiryDate().plusDays(1));
        Instant revoked = c.revokedFrom() == null ? null : time.startOfDay(c.revokedFrom());
        return expiry == null ? revoked : revoked == null ? expiry : expiry.isBefore(revoked) ? expiry : revoked;
    }

    public int run(Instant scheduled, Instant actual) {
        int processed = 0;
        var due = new HashSet<>(credentials.awaitingInvalidity());
        for (var c : credentials.all())
            if (due.contains(c.id()) && !c.replaced()) {
                var at = invalidAt(c);
                if (at != null && !at.isAfter(actual)) {
                    apply(c, at);
                    processed++;
                }
            }
        return processed;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(Credential source, Instant cut) {
        staff.lock(source.staffId());
        var initial = assignments.activeEndingAfter(source.staffId(), cut).stream()
                .map(id -> assignments.find(id, false))
                .toList();
        var selected = initial.stream()
                .filter(a -> a.taskTypeId() != null
                        && sources.requirements(a.taskTypeId()).certificates().stream()
                                .anyMatch(t -> t.equals(source.typeCode())
                                        || t.equals(Long.toString(source.credentialTypeId()))))
                .filter(a -> credentials.staffCredentials(source.staffId()).stream()
                        .noneMatch(c -> c.credentialTypeId() == source.credentialTypeId()
                                && CredentialValidity.covers(
                                        c.issueDate(),
                                        c.expiryDate(),
                                        c.revokedFrom(),
                                        c.replaced(),
                                        a.startsAt().isAfter(cut) ? a.startsAt() : cut,
                                        a.endsAt(),
                                        time)))
                .toList();
        var floors = new TreeSet<Long>();
        for (var a : selected) {
            var s = shifts.shift(a.shiftId(), false);
            if ("FLOOR".equals(s.scopeType()) && s.scopeId() != null) floors.add(s.scopeId());
            else floors.addAll(sources.floors(a.targetType(), a.targetId(), cut));
        }
        assignments.lockFloors(floors);
        selected.stream()
                .map(a -> shifts.shift(a.shiftId(), false).scheduleId())
                .distinct()
                .sorted()
                .forEach(s -> shifts.schedule(s, true));
        selected.stream().map(a -> a.shiftId()).distinct().sorted().forEach(s -> shifts.shift(s, true));
        var affected = new TreeSet<Long>();
        var residents = new TreeSet<Long>();
        for (var candidate : selected) {
            var a = assignments.find(candidate.id(), true);
            if (!Set.of("ACTIVE", "SCHEDULED").contains(a.status())) continue;
            boolean future = a.startsAt().isAfter(cut);
            String next = future ? "CANCELLED" : "ENDED";
            // Nguồn vị trí bắt buộc khi có tác động, không suy ra danh sách người cao tuổi từ dữ liệu thiếu.
            residents.addAll(
                    sources.residents(a.targetType(), a.targetId(), a.startsAt().isAfter(cut) ? a.startsAt() : cut));
            assignments.transition(a, next, future ? null : cut, "Giấy phép/đào tạo hết hiệu lực");
            if (!future) sources.ended(a.id(), a.shiftId());
            affected.add(a.shiftId());
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("ASSIGNMENT_ENDED_BY_CREDENTIAL", false))
                    .entity("M09.ASSIGNMENT", a.id())
                    .before(Map.of("status", a.status()))
                    .after(Map.of("status", next))
                    .basis(new Basis("FR-009", null, source.id(), MDC.get(SchedulerRuns.MDC_KEY)))
                    .details(Map.of("effectiveAt", cut))
                    .build());
            events.publishEvent(new AssignmentChanged(a.shiftId(), a.staffId(), time.now()));
        }
        credentials.markInvalidityApplied(source.id(), time.now());
        if (!affected.isEmpty())
            events.publishEvent(new AssignmentsEndedByCredential(
                    source.staffId(),
                    List.copyOf(affected),
                    List.copyOf(residents),
                    time.now(),
                    notifications.invalidated(source.staffId(), floors, affected, residents, time.now())));
    }
}

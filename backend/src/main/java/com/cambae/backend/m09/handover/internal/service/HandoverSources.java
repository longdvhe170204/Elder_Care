package com.cambae.backend.m09.handover.internal.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.*;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.spi.AbsenceApi;
import com.cambae.backend.m03.api.*;
import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m04.api.*;
import com.cambae.backend.m05.api.AlertHandoverApi;
import com.cambae.backend.m06.api.VitalSignApi;
import com.cambae.backend.m07.api.DoseHandoverApi;
import com.cambae.backend.m09.api.spi.*;
import com.cambae.backend.m09.handover.internal.domain.*;
import com.cambae.backend.m09.shift.internal.domain.WorkShift;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** T027/Q-307: chỉ đọc nguồn song song; không ghi hoặc tự tạo mục, nguồn lỗi hiện rõ. */
@Service
public class HandoverSources {
    private final ObjectProvider<TaskHandoverApi> tasks;
    private final ObjectProvider<ShiftTaskApi> taskCommands;
    private final ObjectProvider<DoseHandoverApi> doses;
    private final ObjectProvider<AlertHandoverApi> alerts;
    private final ObjectProvider<VitalSignApi> vitalSigns;
    private final ObjectProvider<BedStatusApi> beds;
    private final ObjectProvider<AllocationHandoverApi> allocationAlerts;
    private final ObjectProvider<LocationHistoryApi> locations;
    private final ObjectProvider<ResidentStatusApi> statuses;
    private final ObjectProvider<AbsenceApi> absences;
    private final ObjectProvider<HeldBelongingsHandoverPort> belongings;
    private final ObjectProvider<TripDutyPort> trips;
    private final ShiftStore shifts;
    private final BusinessTime time;

    public HandoverSources(
            ObjectProvider<TaskHandoverApi> tasks,
            ObjectProvider<ShiftTaskApi> taskCommands,
            ObjectProvider<DoseHandoverApi> doses,
            ObjectProvider<AlertHandoverApi> alerts,
            ObjectProvider<VitalSignApi> vitalSigns,
            ObjectProvider<BedStatusApi> beds,
            ObjectProvider<AllocationHandoverApi> allocationAlerts,
            ObjectProvider<LocationHistoryApi> locations,
            ObjectProvider<ResidentStatusApi> statuses,
            ObjectProvider<AbsenceApi> absences,
            ObjectProvider<HeldBelongingsHandoverPort> belongings,
            ObjectProvider<TripDutyPort> trips,
            ShiftStore shifts,
            BusinessTime time) {
        this.tasks = tasks;
        this.taskCommands = taskCommands;
        this.doses = doses;
        this.alerts = alerts;
        this.vitalSigns = vitalSigns;
        this.beds = beds;
        this.allocationAlerts = allocationAlerts;
        this.locations = locations;
        this.statuses = statuses;
        this.absences = absences;
        this.belongings = belongings;
        this.trips = trips;
        this.shifts = shifts;
        this.time = time;
    }

    public static <T> T require(ObjectProvider<T> provider) {
        T api = provider.getIfAvailable();
        if (api == null) throw unavailable();
        return api;
    }

    public static BusinessException unavailable() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "HANDOVER_SOURCE_UNAVAILABLE",
                "Chưa đọc được trạng thái nguồn, hãy tải lại trước khi hoàn tất");
    }

    private HandoverSource row(
            String group,
            String type,
            Object id,
            Long resident,
            Long room,
            String description,
            String status,
            String level,
            Instant at) {
        return new HandoverSource(
                group,
                type,
                String.valueOf(id),
                resident,
                room,
                description,
                status,
                CriticalHandoverItem.requiresNote(group, status, level),
                false,
                false,
                level,
                at);
    }

    private List<HandoverSource> safe(String group, String source, Supplier<List<HandoverSource>> read) {
        try {
            return read.get();
        } catch (RuntimeException ex) {
            return List.of(new HandoverSource(
                    group,
                    source,
                    null,
                    null,
                    null,
                    "Chưa đọc được dữ liệu nguồn " + source,
                    null,
                    false,
                    false,
                    true,
                    null,
                    null));
        }
    }

    public List<HandoverSource> list(WorkShift s) {
        long floor = Objects.requireNonNull(s.scopeId());
        Instant now = time.now();
        Instant until = now.isBefore(s.endsAt()) ? now : s.endsAt();
        List<java.util.concurrent.Callable<List<HandoverSource>>> reads =
                List.of(
                        () -> safe(
                                "TASK",
                                "M04.TASK",
                                () -> require(tasks).unclosed(floor, s.id()).stream()
                                        .map(r -> row(
                                                "TASK",
                                                "M04.TASK",
                                                r.id(),
                                                r.residentId(),
                                                r.roomId(),
                                                r.description(),
                                                r.status(),
                                                r.level(),
                                                r.plannedAt()))
                                        .toList()),
                        () -> safe(
                                "DOSE",
                                "M07.DOSE",
                                () -> require(doses).inShift(floor, s.id()).stream()
                                        .map(r -> row(
                                                "DOSE",
                                                "M07.DOSE",
                                                r.key(),
                                                r.residentId(),
                                                r.roomId(),
                                                r.description(),
                                                r.status(),
                                                null,
                                                r.plannedAt()))
                                        .toList()),
                        () -> safe(
                                "ALERT",
                                "M05.ALERT",
                                () -> require(alerts).open(floor, s.id()).stream()
                                        .map(r -> row(
                                                r.group(),
                                                "M05." + r.group(),
                                                r.id(),
                                                r.residentId(),
                                                r.roomId(),
                                                r.description(),
                                                r.status(),
                                                r.level(),
                                                r.createdAt()))
                                        .toList()),
                        () -> safe(
                                "VITAL_SIGN",
                                "M06.VITAL_SIGN",
                                () -> require(vitalSigns).aboveThresholdInRange(floor, s.startsAt(), until).stream()
                                        .map(r -> row(
                                                "VITAL_SIGN",
                                                "M06.VITAL_SIGN",
                                                r.id(),
                                                r.residentId(),
                                                r.roomId(),
                                                r.description(),
                                                "ABNORMAL",
                                                null,
                                                r.measuredAt()))
                                        .toList()),
                        () -> safe(
                                "BED_AWAITING_READINESS",
                                "M03.BED",
                                () -> require(beds).awaitingReadinessOverdue(floor, now).stream()
                                        .map(r -> row(
                                                "BED_AWAITING_READINESS",
                                                "M03.BED",
                                                r.bedId(),
                                                null,
                                                r.roomId(),
                                                r.description(),
                                                "AWAITING_READINESS",
                                                null,
                                                r.awaitingSince()))
                                        .toList()),
                        () -> safe(
                                "ALERT",
                                "M03.ALLOCATION",
                                () -> require(allocationAlerts).atFloor(floor, now).stream()
                                        .map(r -> row(
                                                "ALERT",
                                                "M03.ALLOCATION",
                                                r.id(),
                                                r.residentId(),
                                                r.roomId(),
                                                r.description(),
                                                "AWAITING_CONFIRMATION",
                                                null,
                                                r.createdAt()))
                                        .toList()),
                        () -> safe("RESIDENT_CHANGE", "M01.STATUS", () -> changes(s, until)),
                        () -> safe("RESIDENT_CHANGE", "M09.TRIP", () -> tripChanges(s, until)),
                        () -> safe(
                                "STORED_BELONGING",
                                "M10.BELONGING",
                                () -> require(belongings)
                                        .heldBy(shifts.members(s.id()).stream()
                                                .map(m -> m.staffId())
                                                .toList())
                                        .stream()
                                        .map(r -> row(
                                                "STORED_BELONGING",
                                                "M10.BELONGING",
                                                r.belongingId(),
                                                r.residentId(),
                                                null,
                                                "Đồ gửi đang giữ",
                                                "HELD",
                                                null,
                                                null))
                                        .toList()));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = executor.invokeAll(reads);
            var result = new ArrayList<HandoverSource>();
            for (var f : futures) result.addAll(f.get());
            return List.copyOf(result);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (ExecutionException ex) {
            throw unavailable();
        }
    }

    private List<HandoverSource> changes(WorkShift s, Instant until) {
        var found = new LinkedHashMap<Long, Long>();
        LocalDate first = s.startsAt().atZone(BusinessTime.ZONE).toLocalDate(),
                last = until.atZone(BusinessTime.ZONE).toLocalDate();
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1))
            for (var stay : require(locations).inDay(new LocationRef(s.scopeType(), s.scopeId()), day))
                found.put(stay.residentId(), stay.location().roomId());
        var result = new ArrayList<HandoverSource>();
        for (var e : found.entrySet()) {
            result.addAll(safe("RESIDENT_CHANGE", "M01.STATUS", () -> {
                var changed = new ArrayList<HandoverSource>();
                for (var p : require(statuses)
                        .statusPeriods(
                                e.getKey(),
                                Set.of(
                                        ResidentStatus.IN_RESIDENCE,
                                        ResidentStatus.TEMPORARILY_ABSENT,
                                        ResidentStatus.HOSPITALIZED,
                                        ResidentStatus.OFF_SITE_ACTIVITY),
                                s.startsAt(),
                                until)) {
                    if (!p.from().isBefore(s.startsAt()) && !p.from().isAfter(until))
                        changed.add(row(
                                "RESIDENT_CHANGE",
                                "M01.STATUS",
                                e.getKey() + ":" + p.from(),
                                e.getKey(),
                                e.getValue(),
                                switch (p.status()) {
                                    case IN_RESIDENCE -> "Người cao tuổi nhập viện hoặc trở về";
                                    case TEMPORARILY_ABSENT -> "Người cao tuổi tạm vắng";
                                    case HOSPITALIZED -> "Người cao tuổi điều trị tại bệnh viện";
                                    default -> "Người cao tuổi hoạt động bên ngoài";
                                },
                                p.status().name(),
                                null,
                                p.from()));
                }
                return changed;
            }));
            result.addAll(safe("RESIDENT_CHANGE", "M02.ABSENCE", () -> {
                var absence = require(absences).open(e.getKey());
                if (absence.isPresent()
                        && absence.get().expectedReturnAt() != null
                        && absence.get().expectedReturnAt().isBefore(time.now()))
                    return List.of(row(
                            "RESIDENT_CHANGE",
                            "M02.ABSENCE",
                            absence.get().absenceId(),
                            e.getKey(),
                            e.getValue(),
                            "Quá giờ dự kiến trở về",
                            "OVERDUE_RETURN",
                            null,
                            absence.get().expectedReturnAt()));
                return List.of();
            }));
        }
        return result;
    }

    private List<HandoverSource> tripChanges(WorkShift s, Instant until) {
        var result = new ArrayList<HandoverSource>();
        for (var trip : require(trips).inShift(s.scopeId(), s.startsAt(), until)) {
            if (trip.residentIds().isEmpty() || trip.departedAt() == null) throw unavailable();
            for (long resident : trip.residentIds())
                result.add(row(
                        "RESIDENT_CHANGE",
                        "M09.TRIP",
                        trip.tripId() + ":" + resident,
                        resident,
                        null,
                        "Chuyến đi ngoài viện",
                        trip.expectedReturnAt() != null
                                        && trip.expectedReturnAt().isBefore(time.now())
                                ? "OVERDUE_RETURN"
                                : "OFF_SITE_ACTIVITY",
                        null,
                        trip.departedAt()));
        }
        return result;
    }

    public Map<Long, HandoverSource> current(WorkShift s, List<HandoverItem> items) {
        var result = new LinkedHashMap<Long, HandoverSource>();
        var rows = list(s);
        var taskIds = items.stream()
                .filter(i -> "M04.TASK".equals(i.sourceType()))
                .map(i -> Long.valueOf(i.sourceId()))
                .toList();
        var taskStates = taskIds.isEmpty()
                ? Map.<Long, com.cambae.backend.m04.api.dto.TaskHandoverView>of()
                : require(tasks).currentStatus(taskIds);
        var doseIds = items.stream()
                .filter(i -> "M07.DOSE".equals(i.sourceType()))
                .map(HandoverItem::sourceId)
                .toList();
        var doseStates =
                doseIds.isEmpty() ? Map.<String, String>of() : require(doses).currentStatus(doseIds);
        var alertRefs = items.stream()
                .filter(i -> Set.of("M05.ALERT", "M05.INCIDENT").contains(i.sourceType() == null ? "" : i.sourceType()))
                .map(i -> new AlertHandoverApi.SourceRef(i.group(), Long.parseLong(i.sourceId())))
                .toList();
        var alertStates = alertRefs.isEmpty()
                ? Map.<AlertHandoverApi.SourceRef, com.cambae.backend.m05.api.dto.AlertHandoverView>of()
                : require(alerts).currentStatus(alertRefs);
        var bedIds = items.stream()
                .filter(i -> "M03.BED".equals(i.sourceType()))
                .map(i -> Long.parseLong(i.sourceId()))
                .toList();
        var bedStates =
                bedIds.isEmpty() ? Map.<Long, String>of() : require(beds).currentState(bedIds, time.now());
        var belongingIds = items.stream()
                .filter(i -> "M10.BELONGING".equals(i.sourceType()))
                .map(i -> Long.parseLong(i.sourceId()))
                .toList();
        var held = belongingIds.isEmpty()
                ? Map.<Long, Boolean>of()
                : require(belongings)
                        .stillHeld(
                                belongingIds,
                                shifts.members(s.id()).stream()
                                        .map(m -> m.staffId())
                                        .toList());
        for (var i : items) {
            if (i.sourceId() == null) {
                result.put(
                        i.id(),
                        row(
                                i.group(),
                                i.sourceType(),
                                i.id(),
                                i.residentId(),
                                i.roomId(),
                                i.description(),
                                null,
                                null,
                                null));
                continue;
            }
            if ("M03.BED".equals(i.sourceType()) || "M10.BELONGING".equals(i.sourceType())) {
                String state;
                if ("M03.BED".equals(i.sourceType())) state = bedStates.get(Long.parseLong(i.sourceId()));
                else {
                    var still = held.get(Long.parseLong(i.sourceId()));
                    state = still == null ? null : still ? "HELD" : "NOT_HELD";
                }
                if (state == null) throw unavailable();
                result.put(
                        i.id(),
                        row(
                                i.group(),
                                i.sourceType(),
                                i.sourceId(),
                                i.residentId(),
                                i.roomId(),
                                i.description(),
                                state,
                                null,
                                null));
                continue;
            }
            if ("M04.TASK".equals(i.sourceType())) {
                var r = taskStates.get(Long.valueOf(i.sourceId()));
                if (r == null) throw unavailable();
                result.put(
                        i.id(),
                        row(
                                i.group(),
                                i.sourceType(),
                                i.sourceId(),
                                r.residentId(),
                                r.roomId(),
                                r.description(),
                                r.status(),
                                r.level(),
                                r.plannedAt()));
                continue;
            }
            if ("M07.DOSE".equals(i.sourceType())) {
                var status = doseStates.get(i.sourceId());
                if (status == null) throw unavailable();
                result.put(
                        i.id(),
                        row(
                                i.group(),
                                i.sourceType(),
                                i.sourceId(),
                                i.residentId(),
                                i.roomId(),
                                i.description(),
                                status,
                                null,
                                null));
                continue;
            }
            if (Set.of("M05.ALERT", "M05.INCIDENT").contains(i.sourceType())) {
                var r = alertStates.get(new AlertHandoverApi.SourceRef(i.group(), Long.parseLong(i.sourceId())));
                if (r == null) throw unavailable();
                result.put(
                        i.id(),
                        row(
                                i.group(),
                                i.sourceType(),
                                i.sourceId(),
                                r.residentId(),
                                r.roomId(),
                                r.description(),
                                r.status(),
                                r.level(),
                                r.createdAt()));
                continue;
            }
            var same = rows.stream()
                    .filter(r -> Objects.equals(r.sourceType(), i.sourceType())
                            && Objects.equals(r.sourceId(), i.sourceId()))
                    .findFirst();
            if (same.isPresent()) result.put(i.id(), same.get());
            else if (Set.of("VITAL_SIGN", "RESIDENT_CHANGE").contains(i.group()))
                result.put(
                        i.id(),
                        row(
                                i.group(),
                                i.sourceType(),
                                i.sourceId(),
                                i.residentId(),
                                i.roomId(),
                                i.description(),
                                i.statusAtFinalization(),
                                null,
                                null));
            else throw unavailable();
        }
        return result;
    }

    public void recorded(long handover, HandoverItemInput i) {
        if ("M04.TASK".equals(i.sourceType()))
            require(tasks).recordedInHandover(handover, List.of(Long.valueOf(i.sourceId())));
    }

    public void confirm(long handover, long previous, Long next, long floor, Set<Long> ids, Instant at) {
        require(taskCommands).onHandoverConfirmed(handover, previous, next, ids, at);
        require(alerts).onHandoverConfirmed(handover, previous, next, floor);
    }
}

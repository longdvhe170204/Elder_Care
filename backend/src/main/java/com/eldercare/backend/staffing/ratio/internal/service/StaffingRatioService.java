package com.eldercare.backend.staffing.ratio.internal.service;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.staffing.api.events.ShiftBelowStaffingThreshold;
import com.eldercare.backend.staffing.internal.service.M09TimedTask;
import com.eldercare.backend.staffing.ratio.internal.domain.*;
import com.eldercare.backend.staffing.ratio.internal.persistence.StaffingRatioStore;
import com.eldercare.backend.staffing.shift.internal.domain.WorkShift;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-036/037: khóa ca trước khi chốt lần tính và chuyển cờ cảnh báo cùng transaction. */
@Service
public class StaffingRatioService implements M09TimedTask {
    private final StaffingRatioStore store;
    private final ShiftStore shifts;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<LocationStructureApi> structures;
    private final StaffingRatioSources sources;

    public StaffingRatioService(
            StaffingRatioStore store,
            ShiftStore shifts,
            ParameterApi parameters,
            BusinessTime time,
            ApplicationEventPublisher events,
            ObjectProvider<LocationStructureApi> structures,
            StaffingRatioSources sources) {
        this.store = store;
        this.shifts = shifts;
        this.parameters = parameters;
        this.time = time;
        this.events = events;
        this.structures = structures;
        this.sources = sources;
    }

    public List<StaffingRatioCalculation> history(long shift) {
        return store.history(shift);
    }

    public Optional<StaffingRatioCalculation> latest(long shift) {
        return store.latest(shift);
    }

    /** FR-035: được các lệnh/port nội bộ gọi trong cùng transaction, đọc đầu vào sau khóa lịch và ca. */
    @Transactional
    public List<com.eldercare.backend.staffing.shift.internal.service.port.PublicationWarning> recalculate(
            long id, String event) {
        var initial = shifts.shift(id, false);
        if ("FACILITY".equals(initial.scopeType())
                || Set.of("CLOSED", "CANCELLED").contains(initial.status())) return List.of();
        shifts.schedule(initial.scheduleId(), true);
        var shift = shifts.shift(id, true);
        if (Set.of("CLOSED", "CANCELLED").contains(shift.status())) return List.of();
        if (!shift.endsAt().isAfter(time.now())) return List.of();
        try {
            var result = sources.read(shift, true);
            record(id, event, result);
            return warnings(shift, result);
        } catch (StaffingRatioSources.Unavailable e) {
            return unavailable(shift, e);
        }
    }

    @Transactional(readOnly = true)
    public List<com.eldercare.backend.staffing.shift.internal.service.port.PublicationWarning> preview(long id) {
        var shift = shifts.shift(id, false);
        if ("FACILITY".equals(shift.scopeType())
                || Set.of("CLOSED", "CANCELLED").contains(shift.status())) return List.of();
        try {
            return warnings(shift, sources.read(shift, false));
        } catch (StaffingRatioSources.Unavailable e) {
            return unavailable(shift, e);
        }
    }

    private List<com.eldercare.backend.staffing.shift.internal.service.port.PublicationWarning> warnings(
            WorkShift shift, StaffingRatioCalculator.Result result) {
        if (result.meetsThreshold()) return List.of();
        return List.of(new com.eldercare.backend.staffing.shift.internal.service.port.PublicationWarning(
                "BELOW_STAFFING_THRESHOLD",
                "Ca dưới ngưỡng phục vụ",
                Map.of(
                        "shiftId",
                        shift.id(),
                        "totalWeight",
                        result.totalWeight(),
                        "staffCount",
                        result.staffCount(),
                        "threshold",
                        result.threshold())));
    }

    private List<com.eldercare.backend.staffing.shift.internal.service.port.PublicationWarning> unavailable(
            WorkShift shift, RuntimeException e) {
        return List.of(new com.eldercare.backend.staffing.shift.internal.service.port.PublicationWarning(
                "STAFFING_RATIO_UNAVAILABLE",
                "Chưa đủ nguồn hoặc cấu hình để tính tỷ lệ phục vụ",
                Map.of("shiftId", shift.id(), "reason", e.getMessage())));
    }

    @Transactional
    public Optional<StaffingRatioCalculation> record(long id, String event, StaffingRatioCalculator.Result result) {
        var initial = shifts.shift(id, false);
        shifts.schedule(initial.scheduleId(), true);
        var shift = shifts.shift(id, true);
        if (Set.of("CLOSED", "CANCELLED").contains(shift.status()) || "FACILITY".equals(shift.scopeType()))
            return Optional.empty();
        var calculation = store.append(id, time.now(), event, result);
        if ("DRAFT".equals(shift.status())) return Optional.of(calculation);
        boolean wasBelow = store.below(id);
        store.flag(id, !result.meetsThreshold());
        if (!result.meetsThreshold() && !wasBelow) {
            boolean manager = nearStart(shift, time.now());
            notify(shift, calculation, false, manager);
            if (manager) store.managerNotified(id, time.now());
        }
        return Optional.of(calculation);
    }

    public Cadence cadence() {
        return Cadence.HOURLY;
    }

    @Transactional
    public void run(Instant scheduledAt, Instant actualAt) {
        var until = actualAt.plus(parameters.read(ParameterCode.CFG_M09_08).asDuration());
        for (long id : store.upcomingBelow(actualAt, until)) {
            var initial = shifts.shift(id, false);
            shifts.schedule(initial.scheduleId(), true);
            var shift = shifts.shift(id, true);
            if (Set.of("CLOSED", "CANCELLED", "DRAFT").contains(shift.status())
                    || !store.below(id)
                    || store.managerNotified(id)
                    || !nearStart(shift, actualAt)) continue;
            store.latest(id).ifPresent(c -> {
                notify(shift, c, true, true);
                store.managerNotified(id, actualAt);
            });
        }
    }

    private boolean nearStart(WorkShift shift, Instant at) {
        return shift.endsAt().isAfter(at)
                && !shift.startsAt()
                        .isAfter(at.plus(
                                parameters.read(ParameterCode.CFG_M09_08).asDuration()));
    }

    private void notify(WorkShift shift, StaffingRatioCalculation c, boolean managerOnly, boolean manager) {
        var recipients = new ArrayList<RecipientGroup>();
        if (!managerOnly) {
            Set<Long> floors;
            if ("FLOOR".equals(shift.scopeType())) floors = Set.of(shift.scopeId());
            else {
                var api = structures.getIfAvailable();
                if (api == null) throw new IllegalStateException("Chưa có nguồn cơ cấu vị trí để gửi cảnh báo tỷ lệ");
                floors = api.floorsInArea(shift.scopeId());
            }
            floors.stream()
                    .sorted()
                    .forEach(f -> recipients.add(new RecipientGroup.FloorManager(f, new Substitution.Default())));
        }
        if (manager) recipients.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        Instant at = time.now();
        var request = new NotificationRequest(
                "008",
                "SHIFT_BELOW_STAFFING_THRESHOLD",
                SourceGroup.OTHER,
                new SourceRecord("M09.SHIFT", Long.toString(shift.id())),
                "SHIFT_BELOW_STAFFING_THRESHOLD:" + c.id() + ":" + (managerOnly ? "MANAGER" : "INITIAL"),
                Optional.of(NotificationLevel.MEDIUM),
                Optional.empty(),
                Optional.ofNullable(shift.scopeId()),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, "Ca dưới ngưỡng phục vụ; cần xem lại nhân lực")),
                Optional.of("Ca dưới ngưỡng phục vụ"),
                Optional.of("Ca dưới ngưỡng phục vụ"),
                Optional.empty(),
                false,
                at,
                Map.of(
                        "shiftId",
                        shift.id(),
                        "calculationId",
                        c.id(),
                        "staffCount",
                        c.staffCount(),
                        "totalWeight",
                        c.totalWeight(),
                        "threshold",
                        c.threshold()),
                false,
                Optional.empty(),
                Optional.empty());
        events.publishEvent(
                new ShiftBelowStaffingThreshold(shift.id(), c.ratio(), c.threshold(), at, List.of(request)));
    }
}

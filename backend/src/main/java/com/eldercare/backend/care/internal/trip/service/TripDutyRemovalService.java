package com.eldercare.backend.care.internal.trip.service;

import static com.eldercare.backend.care.internal.trip.service.TripPreparationService.unavailable;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.care.api.TripDutyCommandApi;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.care.internal.trip.domain.TripDuties;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** FR-035: 015 gọi cùng transaction; gỡ đúng người, không gán nhiệm vụ cho người nhận ca. */
@Service
public class TripDutyRemovalService implements TripDutyCommandApi {
    private final ActivityTripStore trips;
    private final TripPreparationService preparation;
    private final ObjectProvider<ShiftStaffPort> shifts;
    private final TripLifecycle lifecycle;
    private final AuditLogApi audit;
    private final BusinessTime time;

    public TripDutyRemovalService(
            ActivityTripStore trips,
            TripPreparationService preparation,
            ObjectProvider<ShiftStaffPort> shifts,
            TripLifecycle lifecycle,
            AuditLogApi audit,
            BusinessTime time) {
        this.trips = trips;
        this.preparation = preparation;
        this.shifts = shifts;
        this.lifecycle = lifecycle;
        this.audit = audit;
        this.time = time;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeDuty(long staff, long shiftId, Basis basis) {
        var candidates = trips.active().stream()
                .filter(t -> {
                    var d = preparation.duties(t.id());
                    return Objects.equals(d.leaderId(), staff)
                            || d.companionIds().contains(staff);
                })
                .toList();
        if (candidates.isEmpty()) return;
        var source = shifts.getIfAvailable();
        if (source == null) throw unavailable("Chưa có nguồn ca khi gỡ nhiệm vụ chuyến");
        var shift = source.shift(shiftId).orElseThrow(() -> unavailable("Chưa đọc được khoảng ca bị thay đổi"));
        for (var candidate : candidates) {
            var trip = trips.lock(candidate.id());
            if (!trip.plan().plannedDepartureAt().isBefore(shift.endsAt())
                    || !trip.plan().plannedReturnAt().isAfter(shift.startsAt())) continue;
            var before = preparation.duties(trip.id());
            boolean leader = Objects.equals(before.leaderId(), staff);
            if (!leader && !before.companionIds().contains(staff)) continue;
            preparation.replaceDuties(
                    trip,
                    new TripDuties(
                            leader ? null : before.leaderId(),
                            before.companionIds().stream()
                                    .filter(s -> s != staff)
                                    .toList()),
                    "REMOVE_DUTY",
                    null);
            if (leader && "IN_PROGRESS".equals(trip.status())) trips.remoteLeader(trip.id());
            lifecycle.safetyNotice(
                    trip,
                    leader ? "LEADER_REMOVED" : "COMPANION_REMOVED",
                    "DUTY_REMOVED:" + staff + ":" + shiftId,
                    time.now());
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("TRIP_DUTY_REMOVED", false))
                    .entity("M04.TRIP", trip.id())
                    .before(Map.of("staffId", staff))
                    .after(Map.of("shiftId", shiftId, "basis", basis))
                    .build());
        }
    }
}

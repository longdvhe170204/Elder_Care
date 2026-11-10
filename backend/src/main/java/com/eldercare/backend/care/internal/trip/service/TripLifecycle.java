package com.eldercare.backend.care.internal.trip.service;

import static com.eldercare.backend.care.internal.trip.service.TripPreparationService.*;

import com.eldercare.backend.care.api.dto.TripMedicationChangeType;
import com.eldercare.backend.care.api.events.TripChanged;
import com.eldercare.backend.care.api.spi.TripMedicationPreparationPort;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.care.internal.trip.domain.*;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.*;
import com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** FR-034/038a: tác động chuyến kèm lệnh buổi/đăng ký; không sửa bản ghi đánh giá nhóm 3. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class TripLifecycle {
    private final ActivityTripStore trips;
    private final ObjectProvider<TripMedicationPreparationPort> medications;
    private final ObjectProvider<VehicleScheduleApi> vehicles;
    private final ObjectProvider<VehicleScheduleCommandApi> commands;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final com.eldercare.backend.administration.shared.api.StatusHistoryApi histories;
    private final com.eldercare.backend.administration.auditlog.api.AuditLogApi audit;

    public TripLifecycle(
            ActivityTripStore trips,
            ObjectProvider<TripMedicationPreparationPort> medications,
            ObjectProvider<VehicleScheduleApi> vehicles,
            ObjectProvider<VehicleScheduleCommandApi> commands,
            ApplicationEventPublisher events,
            BusinessTime time,
            com.eldercare.backend.administration.shared.api.StatusHistoryApi histories,
            com.eldercare.backend.administration.auditlog.api.AuditLogApi audit) {
        this.trips = trips;
        this.medications = medications;
        this.vehicles = vehicles;
        this.commands = commands;
        this.events = events;
        this.time = time;
        this.histories = histories;
        this.audit = audit;
    }

    /** FR-034: khởi tạo chuyến cùng transaction của lệnh tạo buổi lẻ hoặc từ mẫu. */
    public void schedule(long id, TripPlan p) {
        p.validate();
        trips.initialize(id, p);
        audit.write(com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry.builder()
                .action(new com.eldercare.backend.administration.auditlog.api.dto.AuditAction("TRIP_CREATED", false))
                .entity("M04.TRIP", id)
                .before(Map.of())
                .after(Map.of(
                        "destination",
                        p.destination(),
                        "plannedDepartureAt",
                        p.plannedDepartureAt().toString(),
                        "plannedReturnAt",
                        p.plannedReturnAt().toString(),
                        "transport",
                        p.transport(),
                        "facilityVehicle",
                        p.facilityVehicle()))
                .build());
        events.publishEvent(new TripChanged(
                id,
                "SCHEDULED",
                p.facilityVehicle(),
                p.plannedDepartureAt(),
                p.plannedReturnAt(),
                Optional.empty(),
                "TRIP_SCHEDULED:" + id));
    }

    public void reschedule(Session before, Session after) {
        if (before.tripStatus() == null) return;
        var trip = trips.lock(before.id());
        requireScheduled(trip);
        var p = trip.plan();
        var start = p.plannedDepartureAt().plus(Duration.between(before.startsAt(), after.startsAt()));
        var end = p.plannedReturnAt().plus(Duration.between(before.endsAt(), after.endsAt()));
        if (!end.isAfter(start)) throw conflict("STATE_CHANGED", "Giờ về chuyến phải sau giờ rời");
        if (p.facilityVehicle()) {
            var source = vehicles.getIfAvailable();
            if (source == null) throw unavailable("Chưa có nguồn kiểm lịch xe");
            var result = source.checkChange(trip.id(), start, end);
            if (!result.allowed())
                throw conflict(
                        "VEHICLE_ALREADY_SCHEDULED",
                        "Không dời được lịch xe: " + String.join(", ", result.unmetConditions()));
        }
        trips.initialize(trip.id(), new TripPlan(p.destination(), start, end, p.transport(), p.facilityVehicle()));
        boolean differentDate = !LocalDate.ofInstant(p.plannedDepartureAt(), BusinessTime.ZONE)
                .equals(LocalDate.ofInstant(start, BusinessTime.ZONE));
        for (var participant : trips.participants(trip.id())) {
            if (Set.of("REGISTERED", "ELIGIBLE").contains(participant.status())) {
                medication().onTripChanged(trip.id(), participant.residentId(), TripMedicationChangeType.RESCHEDULE);
                if (differentDate) trips.participant(participant.registrationId(), "REGISTERED", null);
            }
        }
        if (p.facilityVehicle())
            vehicleCommand().followTrip(trip.id(), TripVehicleChangeType.CHANGED, start, end, Optional.empty());
        changed(trips.find(trip.id()).orElseThrow(), "CHANGED");
    }

    public void cancel(Session session) {
        if (session.tripStatus() == null) return;
        var trip = trips.lock(session.id());
        requireScheduled(trip);
        for (var p : trips.participants(trip.id()))
            if (Set.of("REGISTERED", "ELIGIBLE").contains(p.status()))
                medication().onTripChanged(trip.id(), p.residentId(), TripMedicationChangeType.CANCEL_TRIP);
        if (trip.plan().facilityVehicle())
            vehicleCommand()
                    .followTrip(
                            trip.id(),
                            TripVehicleChangeType.CANCELLED,
                            trip.plan().plannedDepartureAt(),
                            trip.plan().plannedReturnAt(),
                            Optional.empty());
        trips.cancel(trip.id());
        histories.closeOpen(com.eldercare.backend.care.internal.trip.domain.TripKinds.DUTIES, trip.id(), time.now());
        changed(trip, "CANCELLED");
    }

    public void registrationChanged(long id, Long resident, boolean cancelled) {
        var trip = trips.find(id);
        if (trip.isEmpty()) return;
        if (cancelled && resident != null)
            medication().onTripChanged(id, resident, TripMedicationChangeType.NOT_ATTENDING);
        changed(trip.get(), "CHANGED");
    }

    public void suspectedInfection(long resident, Instant at) {
        for (var trip : trips.active()) {
            if (!"IN_PROGRESS".equals(trip.status())) continue;
            if (trips.participants(trip.id()).stream()
                    .noneMatch(p -> p.residentId() == resident
                            && Set.of("DEPARTED", "MISSING_ON_RETURN").contains(p.status()))) continue;
            safetyNotice(trip, "INFECTION_SUSPECTED", "INFECTION:" + resident + ":" + at, at);
        }
    }

    public void safetyNotice(Trip trip, String cause, String milestone, Instant at) {
        var recipients = new ArrayList<com.eldercare.backend.common.notification.RecipientGroup>();
        recipients.add(new com.eldercare.backend.common.notification.RecipientGroup.TripLeader(
                trip.id(), new com.eldercare.backend.common.notification.Substitution.Default()));
        if (trip.floorAreaId() != null)
            recipients.add(new com.eldercare.backend.common.notification.RecipientGroup.FloorManager(
                    trip.floorAreaId(), new com.eldercare.backend.common.notification.Substitution.Default()));
        if ("LEADER_REMOVED".equals(cause) && "IN_PROGRESS".equals(trip.status()))
            recipients.add(new com.eldercare.backend.common.notification.RecipientGroup.FacilityRole(
                    com.eldercare.backend.common.Role.NURSING_HOME_MANAGER));
        events.publishEvent(new com.eldercare.backend.care.api.events.TripSafetyNotice(
                trip.id(),
                cause,
                new com.eldercare.backend.care.api.dto.CareEventContext(
                        "M04.TRIP", trip.id(), milestone, at, Optional.ofNullable(trip.floorAreaId()), recipients)));
    }

    private void changed(Trip t, String type) {
        events.publishEvent(new TripChanged(
                t.id(),
                type,
                t.plan().facilityVehicle(),
                t.plan().plannedDepartureAt(),
                t.plan().plannedReturnAt(),
                Optional.empty(),
                "TRIP:" + t.id() + ":" + type + ":" + UUID.randomUUID()));
    }

    private TripMedicationPreparationPort medication() {
        var source = medications.getIfAvailable();
        if (source == null) throw unavailable("Chưa có nguồn thuốc để báo chuyến thay đổi");
        return source;
    }

    private VehicleScheduleCommandApi vehicleCommand() {
        var source = commands.getIfAvailable();
        if (source == null) throw unavailable("Chưa có nguồn cập nhật lịch xe");
        return source;
    }

    private void requireScheduled(Trip trip) {
        if (!"SCHEDULED".equals(trip.status()))
            throw conflict("SESSION_ALREADY_STARTED", "Không dời hoặc hủy chuyến đã rời viện");
    }
}

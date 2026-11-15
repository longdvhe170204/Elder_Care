package com.eldercare.backend.care.internal.trip.service;

import static com.eldercare.backend.care.internal.trip.service.TripPreparationService.*;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.events.ActivityCostSourceBilled;
import com.eldercare.backend.care.api.events.TripChanged;
import com.eldercare.backend.care.api.spi.*;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.trip.domain.*;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.*;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.roombedallocation.api.*;
import com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** FR-040/041: kiểm đầy đủ trước ghi; 001 khóa người cao tuổi trước khi khóa chuyến theo L-03. */
@Service
public class TripDepartureService {
    private final ActivityTripStore trips;
    private final TripPreparationService preparation;
    private final CurrentUser user;
    private final ResidentStatusApi status;
    private final ObjectProvider<TripMedicationPreparationPort> medication;
    private final ObjectProvider<ActivityHealthGatePort> health;
    private final ObjectProvider<VehicleScheduleApi> vehicles;
    private final ObjectProvider<VehicleScheduleCommandApi> vehicleCommands;
    private final RecordEventApi records;
    private final SimpleRecordApi simple;
    private final ParameterApi parameters;
    private final DayCareAttendanceApi attendance;
    private final ContractReadApi contracts;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final JsonMapper json;

    public TripDepartureService(
            ActivityTripStore trips,
            TripPreparationService preparation,
            CurrentUser user,
            ResidentStatusApi status,
            ObjectProvider<TripMedicationPreparationPort> medication,
            ObjectProvider<ActivityHealthGatePort> health,
            ObjectProvider<VehicleScheduleApi> vehicles,
            ObjectProvider<VehicleScheduleCommandApi> vehicleCommands,
            RecordEventApi records,
            SimpleRecordApi simple,
            ParameterApi parameters,
            DayCareAttendanceApi attendance,
            ContractReadApi contracts,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            JsonMapper json) {
        this.trips = trips;
        this.preparation = preparation;
        this.user = user;
        this.status = status;
        this.medication = medication;
        this.health = health;
        this.vehicles = vehicles;
        this.vehicleCommands = vehicleCommands;
        this.records = records;
        this.simple = simple;
        this.parameters = parameters;
        this.attendance = attendance;
        this.contracts = contracts;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.json = json;
    }

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void depart(Target target, long id, List<Long> selected, Reason substituteReason, Instant deviceAt) {
        apply(id, selected, substituteReason, deviceAt);
    }

    /** Dùng chung cho lệnh trực tuyến và bản ghi ngoại tuyến {@code M04.TRIP_DEPARTURE} đã qua kiểm quyền của 002 (Q-01). */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void apply(long id, List<Long> selected, Reason substituteReason, Instant deviceAt) {
        if (selected == null
                || selected.isEmpty()
                || selected.stream().anyMatch(Objects::isNull)
                || new HashSet<>(selected).size() != selected.size())
            throw new InvalidValueException("Cần danh sách người rời viện không trùng");
        var trip = trips.find(id).orElseThrow(NotFoundException::new);
        authority(trip, substituteReason);
        var at = deviceAt == null ? time.now() : deviceAt;
        if (at.isAfter(trip.plan().plannedReturnAt()))
            throw conflict("STATE_CHANGED", "Đã quá giờ về dự kiến, không điểm danh rời viện thêm");
        if (at.isAfter(time.now())) throw new InvalidValueException("Thời điểm thiết bị không được ở tương lai");
        if (at.isBefore(trip.plan()
                .plannedDepartureAt()
                .minus(parameters.read(ParameterCode.CFG_M04_13).asDuration())))
            throw conflict("DEPARTURE_TOO_EARLY", "Chưa đến thời gian được điểm danh rời viện");
        var participants = trips.participants(id);
        var ids = selected.stream().sorted().toList();
        var source = medication.getIfAvailable();
        var gate = health.getIfAvailable();
        // Thiếu trưởng đoàn được báo trước thiếu nguồn để Trưởng tầng phân công lại.
        if (source == null || gate == null) throw unavailable("Chưa có nguồn thuốc hoặc khoanh vùng của chuyến");
        if (trip.plan().facilityVehicle()) {
            var vehicle = vehicles.getIfAvailable();
            if (vehicle == null) throw unavailable("Chưa có nguồn lịch xe 019");
            if (!vehicle.hasBookedSchedule(id, trip.plan().plannedDepartureAt()))
                throw conflict("VEHICLE_SCHEDULE_REQUIRED", "Chưa có lịch xe");
            if (vehicleCommands.getIfAvailable() == null) throw unavailable("Chưa có nguồn chuyển trạng thái lịch xe");
        }
        for (long resident : ids) {
            requireEligible(participants, resident);
            if (gate.suspectedInfection(resident, time.now()))
                throw conflict("INFECTION_SUSPECTED", "Người cao tuổi nghi nhiễm");
            if (gate.residentIsolated(resident, time.now()))
                throw conflict("ISOLATION_ACTIVE", "Người cao tuổi đang trong vùng khoanh");
            if (!source.isPrepared(resident, id))
                throw conflict("MEDICATION_NOT_HANDLED", "Chưa chuẩn bị thuốc mang theo");
            var date = LocalDate.ofInstant(at, BusinessTime.ZONE);
            boolean dayCare = contracts
                    .contentAtDate(resident, date)
                    .flatMap(c -> contracts.contract(c.contractId()))
                    .map(c -> c.residenceType() == ResidenceType.DAY_CARE)
                    .orElse(false);
            if (dayCare
                    && attendance
                            .onDate(resident, date)
                            .filter(a -> a.status().name().equals("PRESENT"))
                            .isEmpty())
                throw conflict("RESIDENT_STATUS_INVALID", "Người bán trú chưa điểm danh đến trong ngày");
        }
        // Các lệnh 001 lấy khóa hồ sơ và trạng thái chăm sóc theo thứ tự tăng; lỗi ở bất kỳ người nào hoàn tác cả lô.
        for (long resident : ids) {
            var details = json.createObjectNode()
                    .put("tripId", id)
                    .put("at", at.toString())
                    .put("expectedReturnAt", trip.plan().plannedReturnAt().toString());
            status.execute(
                    resident,
                    StatusCommand.ROLL_CALL_DEPARTURE,
                    user,
                    Optional.empty(),
                    details,
                    new Basis(null, null, id, null),
                    deviceAt);
        }
        var locked = trips.lock(id);
        if (locked.version() != trip.version())
            throw conflict("STATE_CHANGED", "Chuyến vừa thay đổi, tải lại để điểm danh");
        authority(locked, substituteReason);
        var current = trips.participants(id);
        for (long resident : ids) {
            var p = requireEligible(current, resident);
            var record = records.add(
                    TripKinds.ATTENDANCE,
                    new EntityRef("M01.RESIDENT", resident),
                    new NewRecordEvent<>(
                            new TripKinds.Attendance(
                                    id,
                                    p.registrationId(),
                                    resident,
                                    "DEPARTURE",
                                    at,
                                    substituteReason == null ? null : substituteReason.value()),
                            null,
                            user.accountId(),
                            false,
                            at,
                            deviceAt,
                            time.now(),
                            locked.floorAreaId(),
                            // Lần rời viện sau khi Hủy ghi nhận (FR-031a) cần khóa riêng; chạy trùng vẫn bị chặn dưới
                            // khóa chuyến.
                            "TRIP_DEPARTURE:" + p.registrationId() + ":" + trips.departureCount(p.registrationId())));
            if (!trips.markDepartedIfEligible(p.registrationId()))
                throw conflict("STATE_CHANGED", "Đăng ký vừa thay đổi, tải lại để điểm danh");
            var activity = simple.require(ActivityKinds.ACTIVITIES, locked.activityId())
                    .content();
            if (activity.charged())
                events.publishEvent(new ActivityCostSourceBilled(
                        "ACTIVITY_ATTENDANCE",
                        Long.toString(record.id()),
                        "OCCURRENCE",
                        resident,
                        Optional.ofNullable(activity.serviceId()),
                        BigDecimal.ONE,
                        LocalDate.ofInstant(at, BusinessTime.ZONE),
                        time.now(),
                        "TRIP_DEPARTURE:" + record.id()));
        }
        if ("SCHEDULED".equals(locked.status())) {
            trips.start(id);
            if (locked.plan().facilityVehicle())
                vehicleCommands
                        .getObject()
                        .followTrip(
                                id,
                                TripVehicleChangeType.FIRST_DEPARTURE,
                                locked.plan().plannedDepartureAt(),
                                locked.plan().plannedReturnAt(),
                                Optional.of(at));
            events.publishEvent(new TripChanged(
                    id,
                    "FIRST_DEPARTURE",
                    locked.plan().facilityVehicle(),
                    locked.plan().plannedDepartureAt(),
                    locked.plan().plannedReturnAt(),
                    Optional.of(at),
                    "TRIP_FIRST_DEPARTURE:" + id));
        } else
            events.publishEvent(new TripChanged(
                    id,
                    "CHANGED",
                    locked.plan().facilityVehicle(),
                    locked.plan().plannedDepartureAt(),
                    locked.plan().plannedReturnAt(),
                    Optional.of(at),
                    "TRIP_LATER_DEPARTURE:" + id + ":" + ids));
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("TRIP_DEPARTED", false))
                .entity("M04.TRIP", id)
                .before(Map.of("status", trip.status()))
                .after(Map.of("residentIds", ids, "at", at.toString()))
                .reason(substituteReason)
                .build());
    }

    private void authority(Trip trip, Reason reason) {
        if (!Set.of("SCHEDULED", "IN_PROGRESS").contains(trip.status()))
            throw conflict("STATE_CHANGED", "Chuyến đã kết thúc");
        Long leader = preparation.duties(trip.id()).leaderId();
        if (leader == null) throw conflict("TRIP_LEADER_REQUIRED", "Chưa có trưởng đoàn");
        if (user.staffId().filter(leader::equals).isPresent()) return;
        if (!user.roles().contains(Role.FLOOR_MANAGER))
            throw new PermissionDeniedException("Chỉ trưởng đoàn hoặc Trưởng tầng làm thay");
        if (reason == null) throw new InvalidValueException("Trưởng tầng làm thay cần lý do");
    }

    private TripParticipant requireEligible(List<TripParticipant> participants, long resident) {
        var p = participants.stream()
                .filter(r -> r.residentId() == resident)
                .findFirst()
                .orElseThrow(NotFoundException::new);
        if (!"ELIGIBLE".equals(p.status()))
            throw conflict("ELIGIBILITY_NOT_PASSED", "Người tham gia chưa Được đi hoặc đã điểm danh");
        return p;
    }
}

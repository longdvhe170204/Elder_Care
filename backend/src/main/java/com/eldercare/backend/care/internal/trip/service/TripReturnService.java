package com.eldercare.backend.care.internal.trip.service;

import static com.eldercare.backend.care.internal.trip.service.TripPreparationService.*;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.dto.TripInterval;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.api.spi.TripMedicationPreparationPort;
import com.eldercare.backend.care.internal.attendance.service.AttendanceSources;
import com.eldercare.backend.care.internal.session.service.LateReturnConsentService;
import com.eldercare.backend.care.internal.trip.domain.*;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.roombedallocation.api.VehicleScheduleCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 014 T020 (US5): điểm danh về, gia hạn, chuyển trưởng đoàn, kết thúc điểm danh về (FR-035a, FR-043, FR-044, FR-047, FR-050).
 * Lệnh 001 khóa người cao tuổi trước khi khóa chuyến như rời viện (L-03); diễn biến sự cố qua TripIncidentPort cùng transaction.
 */
@Service
public class TripReturnService {
    static final String PARTICIPANT_SOURCE = "M04.TRIP_PARTICIPANT";

    private final ActivityTripStore trips;
    private final TripPreparationService preparation;
    private final StatusHistoryApi histories;
    private final AccountApi accounts;
    private final CurrentUser user;
    private final ResidentStatusApi status;
    private final ObjectProvider<TripMedicationPreparationPort> medications;
    private final ObjectProvider<VehicleScheduleCommandApi> vehicleCommands;
    private final RecordEventApi records;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final JsonMapper json;
    private final LateReturnConsentService consents;
    private final TripMissingService missing;
    private final TripAttendanceTimes attendanceTimes;
    private final AttendanceSources attendanceSources;
    private final ResidentApi residents;

    public TripReturnService(
            ActivityTripStore trips,
            TripPreparationService preparation,
            StatusHistoryApi histories,
            AccountApi accounts,
            CurrentUser user,
            ResidentStatusApi status,
            ObjectProvider<TripMedicationPreparationPort> medications,
            ObjectProvider<VehicleScheduleCommandApi> vehicleCommands,
            RecordEventApi records,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            JsonMapper json,
            LateReturnConsentService consents,
            TripMissingService missing,
            TripAttendanceTimes attendanceTimes,
            AttendanceSources attendanceSources,
            ResidentApi residents) {
        this.trips = trips;
        this.preparation = preparation;
        this.histories = histories;
        this.accounts = accounts;
        this.user = user;
        this.status = status;
        this.medications = medications;
        this.vehicleCommands = vehicleCommands;
        this.records = records;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.json = json;
        this.consents = consents;
        this.missing = missing;
        this.attendanceTimes = attendanceTimes;
        this.attendanceSources = attendanceSources;
        this.residents = residents;
    }

    /** FR-044, FR-047, FR-050: người đã ở viện; về muộn sau khi chuyến Đã về chỉ cho người Thiếu khi về. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void returnResidents(Target target, long id, List<Long> selected, Instant deviceAt) {
        apply(id, selected, deviceAt);
    }

    /** Dùng chung cho lệnh trực tuyến và bản ghi ngoại tuyến {@code M04.TRIP_RETURN} đã qua kiểm quyền của 002 (Q-01). */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void apply(long id, List<Long> selected, Instant deviceAt) {
        var ids = distinct(selected, "Cần danh sách người điểm danh về không trùng");
        var trip = trips.find(id).orElseThrow(NotFoundException::new);
        if (!Set.of("IN_PROGRESS", "RETURNED").contains(trip.status()))
            throw conflict("STATE_CHANGED", "Chuyến chưa rời viện hoặc đã hủy");
        authority(trip);
        var participants = trips.participants(id);
        for (long resident : ids) requireOutside(participants, resident, trip.status());
        var source = medication();
        // Ghi ngoại tuyến (Q-24, Q-01): thời điểm trên thiết bị vào lịch sử trạng thái 001 (đổi api duyệt 2026-11-11)
        var at = deviceAt == null ? time.now() : deviceAt;
        for (long resident : ids)
            status.execute(
                    resident,
                    StatusCommand.ROLL_CALL_RETURN,
                    user,
                    Optional.empty(),
                    json.createObjectNode().put("tripId", id).put("at", at.toString()),
                    new Basis(null, null, id, null),
                    deviceAt);
        var locked = trips.lock(id);
        var current = trips.participants(id);
        for (long resident : ids) {
            var p = requireOutside(current, resident, locked.status());
            boolean wasMissing = "MISSING_ON_RETURN".equals(p.status());
            records.add(
                    TripKinds.ATTENDANCE,
                    new EntityRef("M01.RESIDENT", resident),
                    new NewRecordEvent<>(
                            new TripKinds.Attendance(id, p.registrationId(), resident, "RETURN", at, null),
                            null,
                            user.accountId(),
                            false,
                            at,
                            deviceAt,
                            time.now(),
                            locked.floorAreaId(),
                            "TRIP_RETURN:" + p.registrationId()));
            if (!trips.markReturnedIfOut(p.registrationId()))
                throw conflict("STATE_CHANGED", "Người tham gia vừa thay đổi, tải lại để điểm danh");
            var departed = attendanceTimes
                    .departedAt(p.registrationId())
                    .orElse(locked.plan().plannedDepartureAt());
            source.recalculateInterval(id, resident, new TripInterval(departed, at));
            if (wasMissing && trips.missingIncident(p.registrationId()).isPresent())
                missing.foundReturned(p.registrationId(), resident, id, at);
            else if (wasMissing)
                events.publishEvent(new AlertProgressRequested(
                        PARTICIPANT_SOURCE,
                        p.registrationId(),
                        "FOUND_RETURNED",
                        "Đã tìm thấy, đã điểm danh về viện",
                        "TRIP_FOUND_RETURNED:" + p.registrationId()));
        }
        events.publishEvent(new TripChanged(
                id,
                "CHANGED",
                locked.plan().facilityVehicle(),
                locked.plan().plannedDepartureAt(),
                locked.plan().plannedReturnAt(),
                Optional.of(at),
                "TRIP_RETURN:" + id + ":" + ids + ":" + at));
        log(id, "TRIP_RETURNED", Map.of("residentIds", ids, "at", at.toString()), null);
    }

    /**
     * Q-286 (014 T011, chủ dự án chốt 2026-11-11): bán trú không ở lại qua đêm, nên gia hạn sang ngày sau ngày rời viện dự
     * kiến bị từ chối khi chuyến còn người bán trú đang ở ngoài; lỗi nêu tên người đó.
     */
    private void requireNoDayCareOvernight(Trip trip, long id, Instant newReturnAt) {
        var tripDay = java.time.LocalDate.ofInstant(trip.plan().plannedDepartureAt(), BusinessTime.ZONE);
        if (!java.time.LocalDate.ofInstant(newReturnAt, BusinessTime.ZONE).isAfter(tripDay)) return;
        var dayCare = trips.participants(id).stream()
                .filter(p -> Set.of("DEPARTED", "MISSING_ON_RETURN").contains(p.status()))
                .map(TripParticipant::residentId)
                .filter(resident -> attendanceSources.dayCare(resident, tripDay))
                .toList();
        if (dayCare.isEmpty()) return;
        var names = dayCare.stream()
                .map(resident -> residents.identity(resident).fullName())
                .collect(java.util.stream.Collectors.joining(", "));
        throw new BusinessException(
                org.springframework.http.HttpStatus.CONFLICT,
                "DAY_CARE_OVERNIGHT_NOT_ALLOWED",
                "Người bán trú không ở lại qua đêm (Q-286), không gia hạn sang ngày sau: " + names,
                Map.of("residentIds", dayCare));
    }

    /** FR-043: chỉ khi Đang đi, kèm lý do; 006 nhận khoảng mới của người đang ở ngoài, 011/019 qua TripChanged. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void extend(Target target, long id, Instant newReturnAt, Reason reason) {
        if (reason == null) throw new InvalidValueException("Gia hạn giờ về cần lý do");
        if (newReturnAt == null) throw new InvalidValueException("Cần giờ về dự kiến mới");
        var trip = trips.lock(id);
        if (!"IN_PROGRESS".equals(trip.status())) throw conflict("STATE_CHANGED", "Chỉ gia hạn khi chuyến Đang đi");
        authority(trip);
        var previous = trip.plan().plannedReturnAt();
        if (!newReturnAt.isAfter(previous) || !newReturnAt.isAfter(time.now()))
            throw new InvalidValueException("Giờ về mới phải sau giờ về dự kiến hiện tại và sau thời điểm hiện tại");
        requireNoDayCareOvernight(trip, id, newReturnAt);
        var source = medication();
        records.add(
                TripKinds.EXTENSIONS,
                new EntityRef("M04.ACTIVITY_SESSION", id),
                new NewRecordEvent<>(
                        new TripKinds.Extension(id, previous, newReturnAt, reason.value()),
                        null,
                        user.accountId(),
                        false,
                        time.now(),
                        null,
                        time.now(),
                        trip.floorAreaId(),
                        null));
        trips.extend(id, newReturnAt);
        consents.tripExtended(id);
        for (var p : trips.participants(id))
            if ("DEPARTED".equals(p.status()))
                source.recalculateInterval(
                        id,
                        p.residentId(),
                        new TripInterval(
                                attendanceTimes
                                        .departedAt(p.registrationId())
                                        .orElse(trip.plan().plannedDepartureAt()),
                                newReturnAt));
        if (trip.plan().facilityVehicle())
            vehicleCommand()
                    .followTrip(
                            id,
                            TripVehicleChangeType.EXTENDED,
                            trip.plan().plannedDepartureAt(),
                            newReturnAt,
                            Optional.empty());
        events.publishEvent(new TripChanged(
                id,
                "EXTENDED",
                trip.plan().facilityVehicle(),
                trip.plan().plannedDepartureAt(),
                newReturnAt,
                Optional.empty(),
                "TRIP_EXTENDED:" + id + ":" + newReturnAt));
        log(
                id,
                "TRIP_RETURN_EXTENDED",
                Map.of("previousReturnAt", previous.toString(), "newReturnAt", newReturnAt.toString()),
                reason);
    }

    /** FR-035a: người nhận phải đang đi cùng và có vai trò trưởng đoàn; trưởng đoàn cũ thành người đi cùng. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void transferLeader(Target target, long id, Long newLeader, Reason reason) {
        if (reason == null) throw new InvalidValueException("Chuyển trưởng đoàn cần lý do");
        if (newLeader == null) throw new InvalidValueException("Cần trưởng đoàn mới");
        var trip = trips.lock(id);
        if (!"IN_PROGRESS".equals(trip.status()))
            throw conflict("STATE_CHANGED", "Chỉ chuyển trưởng đoàn khi chuyến Đang đi");
        authority(trip);
        var duties = preparation.duties(id);
        if (!duties.companionIds().contains(newLeader))
            throw conflict("STATE_CHANGED", "Trưởng đoàn mới phải là người đi cùng của chuyến");
        var account = accounts.byStaff(newLeader)
                .filter(a -> a.status() == AccountStatus.ACTIVE)
                .orElseThrow(() -> conflict("STATE_CHANGED", "Nhân viên chưa có tài khoản Hoạt động"));
        if (Collections.disjoint(account.roles(), Set.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.CAREGIVER)))
            throw conflict("ROLE_NOT_ELIGIBLE", "Trưởng đoàn phải là Trưởng tầng, Điều dưỡng hoặc Nhân viên chăm sóc");
        var companions = new ArrayList<>(duties.companionIds());
        companions.remove(newLeader);
        if (duties.leaderId() != null) companions.add(duties.leaderId());
        preparation.replaceDuties(trip, new TripDuties(newLeader, companions), "TRANSFER_LEADER", reason);
        var after = new LinkedHashMap<String, Object>();
        after.put("leaderId", newLeader);
        after.put("previousLeaderId", duties.leaderId());
        log(id, "TRIP_LEADER_TRANSFERRED", after, reason);
    }

    /**
     * FR-047, FR-050: người Đã rời viện chưa về chuyển Thiếu khi về và yêu cầu 007 tạo cảnh báo Khẩn cấp cho từng người;
     * không tạo sự cố. Người Rời đoàn không tính thiếu.
     */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void finishReturn(Target target, long id, Instant deviceAt) {
        if (deviceAt != null) throw unavailable("Kết thúc điểm danh về chỉ thực hiện trực tuyến");
        var trip = trips.lock(id);
        if (!"IN_PROGRESS".equals(trip.status())) throw conflict("STATE_CHANGED", "Chuyến không ở Đang đi");
        authority(trip);
        var now = time.now();
        var missing = new ArrayList<Long>();
        for (var p : trips.participants(id)) {
            if (!"DEPARTED".equals(p.status()) || !trips.markMissingIfDeparted(p.registrationId())) continue;
            missing.add(p.residentId());
            events.publishEvent(new AlertRequested(
                    "MISSING_ON_RETURN",
                    NotificationLevel.URGENT,
                    p.residentId(),
                    "M04.TRIP:" + id,
                    SourceType.ACTIVITY_SESSION,
                    id,
                    new CareEventContext(
                            PARTICIPANT_SOURCE,
                            p.registrationId(),
                            "MISSING_ON_RETURN:" + p.registrationId(),
                            now,
                            Optional.ofNullable(trip.floorAreaId()),
                            recipients(trip))));
        }
        trips.finishReturn(id, now, user.staffId().orElse(null));
        histories.closeOpen(TripKinds.DUTIES, id, now);
        if (trip.plan().facilityVehicle())
            vehicleCommand()
                    .followTrip(
                            id,
                            TripVehicleChangeType.RETURN_FINISHED,
                            trip.plan().plannedDepartureAt(),
                            trip.plan().plannedReturnAt(),
                            Optional.of(now));
        events.publishEvent(new TripChanged(
                id,
                "RETURN_FINISHED",
                trip.plan().facilityVehicle(),
                trip.plan().plannedDepartureAt(),
                trip.plan().plannedReturnAt(),
                Optional.of(now),
                "TRIP_RETURN_FINISHED:" + id));
        log(id, "TRIP_RETURN_FINISHED", Map.of("missingResidentIds", missing), null);
    }

    private List<RecipientGroup> recipients(Trip trip) {
        var list = new ArrayList<RecipientGroup>();
        list.add(new RecipientGroup.TripLeader(trip.id(), new Substitution.Default()));
        if (trip.floorAreaId() != null)
            list.add(new RecipientGroup.FloorManager(trip.floorAreaId(), new Substitution.Default()));
        return list;
    }

    /** Trưởng đoàn hiện hành (sau khi Đã về: trưởng đoàn cuối) hoặc Trưởng tầng. */
    private void authority(Trip trip) {
        var rows = histories.list(TripKinds.DUTIES, trip.id());
        Long leader = rows.isEmpty() ? null : rows.getLast().details().leaderId();
        if (leader != null && user.staffId().filter(leader::equals).isPresent()) return;
        if (!user.roles().contains(Role.FLOOR_MANAGER))
            throw new PermissionDeniedException("Chỉ trưởng đoàn hoặc Trưởng tầng thực hiện lệnh chuyến");
    }

    private static TripParticipant requireOutside(
            List<TripParticipant> participants, long resident, String tripStatus) {
        var p = participants.stream()
                .filter(r -> r.residentId() == resident)
                .findFirst()
                .orElseThrow(NotFoundException::new);
        boolean allowed = "RETURNED".equals(tripStatus)
                ? "MISSING_ON_RETURN".equals(p.status())
                : Set.of("DEPARTED", "MISSING_ON_RETURN").contains(p.status());
        if (!allowed) throw conflict("STATE_CHANGED", "Người tham gia không ở ngoài viện theo chuyến này");
        return p;
    }

    private static List<Long> distinct(List<Long> selected, String message) {
        if (selected == null
                || selected.isEmpty()
                || selected.stream().anyMatch(Objects::isNull)
                || new HashSet<>(selected).size() != selected.size()) throw new InvalidValueException(message);
        return selected.stream().sorted().toList();
    }

    private TripMedicationPreparationPort medication() {
        var source = medications.getIfAvailable();
        if (source == null) throw unavailable("Chưa có nguồn thuốc để tính lại khoảng mang theo");
        return source;
    }

    private VehicleScheduleCommandApi vehicleCommand() {
        var source = vehicleCommands.getIfAvailable();
        if (source == null) throw unavailable("Chưa có nguồn cập nhật lịch xe");
        return source;
    }

    private void log(long id, String action, Map<String, Object> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, reason != null))
                .entity("M04.TRIP", id)
                .before(Map.of())
                .after(after)
                .reason(reason)
                .build());
    }
}

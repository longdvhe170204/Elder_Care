package com.eldercare.backend.care.internal.trip.service;

import static com.eldercare.backend.care.internal.trip.domain.TripKinds.*;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.api.dto.TripMedicationChangeType;
import com.eldercare.backend.care.api.spi.*;
import com.eldercare.backend.care.internal.trip.domain.*;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T017, FR-035/036: quyền vai trò và tầng đánh giá được xét riêng với tầng tổ chức chuyến. */
@Service
public class TripPreparationService {
    private final ActivityTripStore trips;
    private final StatusHistoryApi histories;
    private final RecordEventApi records;
    private final CurrentUser user;
    private final AccountApi accounts;
    private final BedAllocationApi beds;
    private final ObjectProvider<TripStaffSchedulePort> schedules;
    private final ObjectProvider<TripMedicationPreparationPort> medications;
    private final BusinessTime time;
    private final AuditLogApi audit;

    public TripPreparationService(
            ActivityTripStore trips,
            StatusHistoryApi histories,
            RecordEventApi records,
            CurrentUser user,
            AccountApi accounts,
            BedAllocationApi beds,
            ObjectProvider<TripStaffSchedulePort> schedules,
            ObjectProvider<TripMedicationPreparationPort> medications,
            BusinessTime time,
            AuditLogApi audit) {
        this.trips = trips;
        this.histories = histories;
        this.records = records;
        this.user = user;
        this.accounts = accounts;
        this.beds = beds;
        this.schedules = schedules;
        this.medications = medications;
        this.time = time;
        this.audit = audit;
    }

    public Target target(long id) {
        var trip = trips.find(id).orElseThrow(NotFoundException::new);
        return new Target("M04.TRIP", Long.toString(id), null, trip.floorAreaId());
    }

    public Target assessmentTarget(long id, long resident) {
        trips.find(id).orElseThrow(NotFoundException::new);
        var floor = beds.openAt(resident, time.now()).map(b -> b.floorAreaId()).orElseThrow(NotFoundException::new);
        return new Target("M04.TRIP", null, resident, floor);
    }

    public TripDuties duties(long id) {
        return histories.open(DUTIES, id).map(HistoryRow::details).orElse(new TripDuties(null, List.of()));
    }

    public record Detail(Trip trip, TripDuties duties, List<TripParticipant> participants) {}

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Detail detail(Target target, long id) {
        return new Detail(trips.find(id).orElseThrow(NotFoundException::new), duties(id), trips.participants(id));
    }

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void assign(Target target, long id, Long leader, List<Long> companions) {
        manager();
        var trip = trips.lock(id);
        if (!"SCHEDULED".equals(trip.status()))
            throw conflict("STATE_CHANGED", "Chỉ phân công trước khi chuyến rời viện; đổi trưởng đoàn dùng lệnh riêng");
        if (leader == null) throw new InvalidValueException("Cần trưởng đoàn");
        if (companions != null && companions.stream().anyMatch(Objects::isNull))
            throw new InvalidValueException("Mã người đi cùng không được rỗng");
        var list = companions == null ? List.<Long>of() : List.copyOf(companions);
        if (list.contains(leader) || new HashSet<>(list).size() != list.size())
            throw new InvalidValueException("Nhân viên bị trùng trong phân công");
        checkStaff(leader, true, trip);
        for (long staff : list) checkStaff(staff, false, trip);
        replaceDuties(trip, new TripDuties(leader, list), "ASSIGN", null);
        log(id, "TRIP_ASSIGNED", Map.of("leaderId", leader, "companionIds", list));
    }

    public void replaceDuties(
            Trip trip, TripDuties duties, String command, com.eldercare.backend.common.Reason reason) {
        histories.closeOpen(DUTIES, trip.id(), time.now());
        histories.add(
                DUTIES,
                trip.id(),
                new HistoryEntry<>(
                        HistoryKind.PERIOD,
                        null,
                        "ASSIGNED",
                        duties.leaderId(),
                        time.now(),
                        command,
                        user.accountId(),
                        false,
                        reason,
                        null,
                        null,
                        duties,
                        trip.floorAreaId(),
                        null));
        // Đổi nhiệm vụ phải làm hết hiệu lực ảnh chuyến mà lệnh rời viện đã đọc trước khi khóa.
        trips.dutiesChanged(trip.id());
    }

    private void checkStaff(long staff, boolean leader, Trip trip) {
        var account = accounts.byStaff(staff)
                .filter(a -> a.status() == AccountStatus.ACTIVE)
                .orElseThrow(() -> conflict("STATE_CHANGED", "Nhân viên chưa có tài khoản Hoạt động"));
        if (leader
                && Collections.disjoint(
                        account.roles(), Set.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.CAREGIVER)))
            throw conflict("ROLE_NOT_ELIGIBLE", "Trưởng đoàn phải là Trưởng tầng, Điều dưỡng hoặc Nhân viên chăm sóc");
        var source = schedules.getIfAvailable();
        if (source == null) throw unavailable("Chưa có nguồn ca chồng thời gian chuyến");
        if (!source.hasOverlappingPublishedShift(
                staff, trip.plan().plannedDepartureAt(), trip.plan().plannedReturnAt()))
            throw conflict("NOT_ON_SHIFT", "Nhân viên không có tên trong ca chồng thời gian chuyến");
    }

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void assess(Target target, long id, long resident, String result, String reason) {
        manager();
        if (!Set.of("PASS", "FAIL").contains(result == null ? "" : result))
            throw new InvalidValueException("Kết quả phải là Đạt hoặc Không đạt");
        if ("FAIL".equals(result) && (reason == null || reason.isBlank()))
            throw new InvalidValueException("Không đạt cần lý do");
        var trip = trips.lock(id);
        if (!"SCHEDULED".equals(trip.status())) throw conflict("STATE_CHANGED", "Chuyến không ở Đã lên lịch");
        var p = trips.participants(id).stream()
                .filter(r -> r.residentId() == resident)
                .findFirst()
                .orElseThrow(NotFoundException::new);
        if (!"REGISTERED".equals(p.status()) && !("ELIGIBLE".equals(p.status()) && "FAIL".equals(result)))
            throw conflict("STATE_CHANGED", "Người tham gia đã được đánh giá hoặc đã kết thúc");
        records.add(
                ASSESSMENTS,
                new EntityRef("M01.RESIDENT", resident),
                new NewRecordEvent<>(
                        new Assessment(id, p.registrationId(), resident, result, reason),
                        null,
                        user.accountId(),
                        false,
                        time.now(),
                        null,
                        time.now(),
                        target.floorAreaId(),
                        null));
        if ("PASS".equals(result)) trips.participant(p.registrationId(), "ELIGIBLE", reason);
        else {
            var source = medications.getIfAvailable();
            if (source == null) throw unavailable("Chưa có nguồn thuốc để báo người Không đi");
            source.onTripChanged(id, resident, TripMedicationChangeType.NOT_ATTENDING);
            trips.notGoing(p.registrationId(), reason);
        }
        log(id, "TRIP_ELIGIBILITY_ASSESSED", Map.of("residentId", resident, "result", result));
    }

    private void manager() {
        if (!user.roles().contains(Role.FLOOR_MANAGER))
            throw new PermissionDeniedException("Chỉ Trưởng tầng phân công và đánh giá chuyến");
    }

    private void log(long id, String action, Map<String, Object> after) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, false))
                .entity("M04.TRIP", id)
                .before(Map.of())
                .after(after)
                .build());
    }

    static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    static BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", message);
    }
}

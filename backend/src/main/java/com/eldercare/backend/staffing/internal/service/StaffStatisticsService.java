package com.eldercare.backend.staffing.internal.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.staffing.api.StaffStatisticsApi;
import com.eldercare.backend.staffing.api.dto.*;
import com.eldercare.backend.staffing.credential.internal.domain.CredentialValidity;
import com.eldercare.backend.staffing.credential.internal.persistence.CredentialStore;
import com.eldercare.backend.staffing.internal.domain.StaffStatisticsRows;
import com.eldercare.backend.staffing.internal.domain.StaffStatisticsRows.Assignment;
import com.eldercare.backend.staffing.internal.persistence.StaffStatisticsStore;
import com.eldercare.backend.staffing.ratio.internal.persistence.StaffingRatioStore;
import com.eldercare.backend.staffing.shift.internal.domain.WorkShift;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-037/037a: dữ liệu lịch sử thật; tổng hợp cả tầng chỉ trả khi toàn bộ phạm vi đó được phép. */
@Service
@Transactional(readOnly = true)
public class StaffStatisticsService implements StaffStatisticsApi {
    private final StaffingRatioStore ratios;
    private final ShiftStore shifts;
    private final StaffStatisticsStore store;
    private final CredentialStore credentials;
    private final ObjectProvider<LocationStructureApi> structures;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final AccountApi accounts;

    public StaffStatisticsService(
            StaffingRatioStore ratios,
            ShiftStore shifts,
            StaffStatisticsStore store,
            CredentialStore credentials,
            ObjectProvider<LocationStructureApi> structures,
            BusinessTime time,
            ParameterApi parameters,
            AuditLogApi audit,
            AccountApi accounts) {
        this.ratios = ratios;
        this.shifts = shifts;
        this.store = store;
        this.credentials = credentials;
        this.structures = structures;
        this.time = time;
        this.parameters = parameters;
        this.audit = audit;
        this.accounts = accounts;
    }

    private Instant[] period(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to))
            throw new IllegalArgumentException("Khoảng ngày báo cáo không hợp lệ");
        return new Instant[] {time.startOfDay(from), time.startOfDay(to.plusDays(1))};
    }

    private Map<Long, WorkShift> shifts(Instant from, Instant to) {
        return shifts.reportShifts(from, to).stream().collect(Collectors.toMap(WorkShift::id, s -> s));
    }

    private boolean allowed(ScopeFilter scope, WorkShift shift) {
        return shift != null && allowed(scope, shift.scopeType(), shift.scopeId());
    }

    private boolean allowed(ScopeFilter scope, String type, Long id) {
        if (scope.facilityWide()) return true;
        if (id == null) return false;
        if ("ROOM".equals(type)) return scope.roomIds().contains(id);
        if ("RESIDENT".equals(type)) return scope.residentIds().contains(id);
        if (scope.floorAreaIds().contains(id)) return true;
        if (scope.floorAreaIds().isEmpty()) return false;
        var api = structures.getIfAvailable();
        if (api == null) throw unavailable("Chưa có nguồn cơ cấu vị trí để lọc thống kê");
        if ("FLOOR".equals(type))
            return api.floor(id)
                    .map(f -> scope.floorAreaIds().contains(f.areaId()))
                    .orElse(false);
        var floors = api.floorsInArea(id);
        return !floors.isEmpty() && scope.floorAreaIds().containsAll(floors);
    }

    public Stream<StaffingRatioView> staffingRatios(ScopeFilter scope, LocalDate from, LocalDate to) {
        var interval = period(from, to);
        var parents = shifts(interval[0], interval[1]);
        var ever = ratios.everBelow(interval[0], interval[1]);
        return ratios.forReport(interval[0], interval[1]).stream()
                .filter(c -> allowed(scope, parents.get(c.shiftId())))
                .map(c -> {
                    var s = parents.get(c.shiftId());
                    return new StaffingRatioView(
                            c.id(),
                            c.shiftId(),
                            s.scopeId(),
                            c.at(),
                            c.ratio(),
                            c.threshold(),
                            c.meetsThreshold(),
                            ever.contains(c.shiftId()),
                            c.staffCount(),
                            c.totalWeight(),
                            s.shiftTemplateId(),
                            s.startsAt(),
                            s.endsAt());
                });
    }

    public Stream<HandoverReportView> handovers(ScopeFilter scope, LocalDate from, LocalDate to) {
        var interval = period(from, to);
        var parents = shifts(interval[0], interval[1]);
        return handoverViews(scope, store.handovers(interval[0], interval[1]), parents::get);
    }

    public Stream<HandoverReportView> openHandovers(ScopeFilter scope) {
        return handoverViews(scope, store.openHandovers(), id -> shifts.find(id).orElse(null));
    }

    private Stream<HandoverReportView> handoverViews(
            ScopeFilter scope, List<StaffStatisticsRows.Handover> rows, Function<Long, WorkShift> parent) {
        var grace = parameters.read(ParameterCode.CFG_M09_05).asDuration();
        var now = time.now();
        return rows.stream()
                .filter(h -> allowed(scope, parent.apply(h.shiftId())))
                .map(h -> {
                    var s = parent.apply(h.shiftId());
                    boolean late = h.preparedAt() == null
                            ? !now.isBefore(s.endsAt())
                            : h.preparedAt().isAfter(s.endsAt());
                    if (h.preparedAt() != null)
                        late |= h.confirmedAt() == null
                                ? !now.isBefore(s.endsAt().plus(grace))
                                : h.confirmedAt().isAfter(s.endsAt().plus(grace));
                    return new HandoverReportView(
                            h.id(),
                            h.shiftId(),
                            s.scopeId(),
                            h.status(),
                            h.preparedAt(),
                            h.confirmedAt(),
                            late,
                            s.endsAt());
                });
    }

    public Stream<ExpiringCredentialView> expiringCredentials(int withinDays) {
        if (withinDays < 0) throw new IllegalArgumentException("Số ngày phải không âm");
        var today = time.today();
        var until = today.plusDays(withinDays);
        var now = time.now();
        return credentials.all().stream()
                .filter(c -> c.expiryDate() != null
                        && !c.expiryDate().isBefore(today)
                        && !c.expiryDate().isAfter(until))
                .filter(c -> CredentialValidity.status(
                                c.issueDate(), c.expiryDate(), c.revokedFrom(), c.replaced(), now, time)
                        == CredentialValidity.Status.VALID)
                .map(c -> new ExpiringCredentialView(c.id(), c.staffId(), c.typeCode(), c.expiryDate(), c.group()));
    }

    public Stream<AssignmentHistoryView> assignmentHistory(ScopeFilter scope, Instant from, Instant to) {
        validatePeriod(from, to);
        var parents = shifts(from, to);
        var rows = new ArrayList<>(store.assignments(from, to));
        rows.addAll(store.shiftDuties(from, to));
        for (var a : store.floorManagers(from, to)) {
            var api = structures.getIfAvailable();
            if (api == null) throw unavailable("Chưa có nguồn cơ cấu vị trí để đọc lịch sử Trưởng tầng");
            String type;
            if (api.floor(a.targetId()).isPresent()) type = "FLOOR";
            else if (api.area(a.targetId()).isPresent()) type = "AREA";
            else {
                if (scope.facilityWide())
                    throw unavailable("Không xác định được tầng/khu vực trong lịch sử Trưởng tầng");
                continue;
            }
            rows.add(new Assignment(a.id(), a.staffId(), null, type, a.targetId(), a.from(), a.to(), a.role()));
        }
        return rows.stream()
                .filter(a -> a.shiftId() == null
                        ? allowed(scope, a.targetType(), a.targetId())
                        : allowed(scope, parents.get(a.shiftId()))
                                || ("ROOM".equals(a.targetType())
                                        && scope.roomIds().contains(a.targetId()))
                                || ("RESIDENT".equals(a.targetType())
                                        && scope.residentIds().contains(a.targetId())))
                .sorted(Comparator.comparing(Assignment::from)
                        .thenComparing(Assignment::role)
                        .thenComparingLong(Assignment::id))
                .map(a -> new AssignmentHistoryView(
                        a.id(), a.staffId(), a.shiftId(), a.targetType(), a.targetId(), a.from(), a.to(), a.role()));
    }

    public Stream<DutyBasisView> nonRoleBases(ScopeFilter scope, Instant from, Instant to) {
        validatePeriod(from, to);
        var rows = new ArrayList<DutyBasisView>();
        var staffByAccount = new HashMap<Long, Long>();
        for (String type : List.of(
                "M09.HANDOVER",
                "M09.SHIFT",
                "M09.SHIFT_STAFF",
                "M09.SHIFT_LEAD_HISTORY",
                "M09.ASSIGNMENT",
                "M09.STAFF_ABSENCE")) {
            long after = 0;
            List<AuditLogRecord> page;
            do {
                page = audit.recordedBetween(type, from, to, after, AuditLogApi.RECORDED_LIMIT);
                for (var entry : page) {
                    var data = entry.details();
                    if (data == null) continue;
                    Object basis = data.getOrDefault("permissionBasis", data.get("basis"));
                    // Không suy quyền quá khứ từ vai trò hiện tại; mã BR/FR không phải căn cứ nhiệm vụ.
                    if (!(basis instanceof String value)
                            || !Set.of(
                                            "SHIFT_LEAD",
                                            "FLOOR_MANAGER_SUBSTITUTE",
                                            "TEMPORARY_RECEIVER",
                                            "ROLE_INVITATION_SENT")
                                    .contains(value)) continue;
                    if (!scope.facilityWide()) {
                        Long shiftId = number(data.get("shiftId"));
                        if (shiftId == null) {
                            Long entityId = number(entry.entityId());
                            if (entityId != null)
                                shiftId = "M09.SHIFT".equals(type)
                                        ? entityId
                                        : store.shiftForAudit(type, entityId).orElse(null);
                        }
                        if (shiftId == null
                                || !allowed(scope, shifts.find(shiftId).orElse(null))) continue;
                    }
                    if (entry.actorAccountId() == null)
                        throw unavailable("Nhật ký căn cứ nhiệm vụ thiếu tài khoản thực hiện");
                    long staff = staffByAccount.computeIfAbsent(
                            entry.actorAccountId(),
                            account -> accounts.byId(account)
                                    .map(a -> a.staffId())
                                    .orElseThrow(
                                            () -> unavailable("Không tìm được nhân viên của nhật ký căn cứ nhiệm vụ")));
                    rows.add(new DutyBasisView(entry.id(), type, entry.entityId(), staff, value, entry.occurredAt()));
                }
                if (!page.isEmpty()) after = page.getLast().id();
            } while (page.size() == AuditLogApi.RECORDED_LIMIT);
        }
        return rows.stream().sorted(Comparator.comparing(DutyBasisView::at).thenComparingLong(DutyBasisView::auditId));
    }

    private static Long number(Object value) {
        try {
            return value == null ? null : Long.valueOf(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void validatePeriod(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to))
            throw new IllegalArgumentException("Khoảng lịch sử không hợp lệ");
    }

    private static BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "STAFF_STATISTICS_SOURCE_UNAVAILABLE", message);
    }
}

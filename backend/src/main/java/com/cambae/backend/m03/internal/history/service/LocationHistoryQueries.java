package com.cambae.backend.m03.internal.history.service;

import com.cambae.backend.common.error.*;
import com.cambae.backend.m01.api.*;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.internal.history.domain.HistoryAbsence;
import com.cambae.backend.m03.internal.history.domain.HistoryStay;
import com.cambae.backend.m03.internal.history.persistence.LocationHistoryStore;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import com.cambae.backend.m15.approval.api.ApprovalRequestApi;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T046: dữ liệu vị trí từ M03, nhãn và khoảng vắng chỉ qua API M01; phạm vi lịch sử chỉ đọc. */
@Service
@Transactional(readOnly = true)
public class LocationHistoryQueries {
    private final LocationHistoryStore store;
    private final ResidentApi residents;
    private final ResidentStatusApi statuses;
    private final ApprovalRequestApi approvals;
    private final PermissionApi permissions;
    private final ScopeApi scope;
    private final CurrentUser user;

    public LocationHistoryQueries(
            LocationHistoryStore store,
            ResidentApi residents,
            ResidentStatusApi statuses,
            ApprovalRequestApi approvals,
            PermissionApi permissions,
            ScopeApi scope,
            CurrentUser user) {
        this.store = store;
        this.residents = residents;
        this.statuses = statuses;
        this.approvals = approvals;
        this.permissions = permissions;
        this.scope = scope;
        this.user = user;
    }

    public List<HistoryStay> residentStays(long id, Instant from, Instant to) {
        var filter = filter(from, to);
        return store.resident(id, from, to).stream()
                .filter(row -> visible(row, filter))
                .toList();
    }

    public List<HistoryStay> residentAt(long id, Instant at) {
        if (at == null) throw new InvalidValueException("Cần thời điểm tra cứu");
        var filter = filter(at, at.plusNanos(1000));
        return store.residentAt(id, at).stream()
                .filter(row -> visible(row, filter))
                .toList();
    }

    public List<LocationView> beds(LocationRef location, Instant from, Instant to) {
        var filter = filter(from, to);
        return store.beds(location).stream()
                .filter(position -> (filter.facilityWide()
                                || filter.floorAreaIds().contains(position.floorId())
                                || filter.roomIds().contains(position.roomId()))
                        && permitted(new Target("M03.BED", Long.toString(position.bedId()), null, position.floorId())))
                .toList();
    }

    public List<HistoryAbsence> absenceDetails(long id, Instant from, Instant to) {
        try {
            return statuses
                    .statusPeriods(
                            id,
                            Set.of(
                                    ResidentStatus.TEMPORARILY_ABSENT,
                                    ResidentStatus.HOSPITALIZED,
                                    ResidentStatus.OFF_SITE_ACTIVITY),
                            from,
                            to)
                    .stream()
                    .map(period -> new HistoryAbsence(
                            id,
                            period.status().name(),
                            period.from().isBefore(from) ? from : period.from(),
                            period.to().filter(end -> end.isBefore(to)).orElse(to)))
                    .filter(period -> period.from().isBefore(period.to()))
                    .toList();
        } catch (UnsupportedOperationException unavailable) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SOURCE_UNAVAILABLE",
                    "M01 chưa cung cấp khoảng trạng thái vắng; chưa thể trả danh sách cùng phòng đầy đủ");
        }
    }

    public List<HistoryStay> locationStays(LocationRef location, Instant from, Instant to, boolean point) {
        var filter = filter(from, point ? from.plusNanos(1000) : to);
        return store.location(location, from, to, point).stream()
                .filter(row -> visible(row, filter))
                .toList();
    }

    public List<HistoryStay> roommateStays(long id, Instant from, Instant to) {
        var filter = filter(from, to);
        // Không trả thông tin người cùng phòng khi A nằm ngoài phạm vi.
        var own = store.resident(id, from, to).stream()
                .filter(row -> visible(row, filter))
                .toList();
        if (own.isEmpty()) return List.of();
        return store.roommates(id, from, to).stream()
                .filter(row -> visible(row, filter))
                .filter(row -> own.stream()
                        .anyMatch(source -> Objects.equals(
                                        source.location().roomId(),
                                        row.location().roomId())
                                && !row.from().isBefore(source.from())
                                && (source.to() == null || !row.to().isAfter(source.to()))))
                .toList();
    }

    public String name(long id) {
        return residents.identity(id).fullName();
    }

    public List<AbsenceInterval> absences(long id, Instant from, Instant to) {
        return absenceDetails(id, from, to).stream()
                .map(period -> new AbsenceInterval(period.from(), period.to()))
                .toList();
    }

    public boolean pending(long id, Instant from, Instant to) {
        return store.backfillRequests(id, from, to).stream()
                .map(approvals::find)
                .flatMap(Optional::stream)
                .anyMatch(request -> Set.of("PENDING_APPROVAL", "APPROVED").contains(request.status()));
    }

    private ScopeFilter filter(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to))
            throw new InvalidValueException("Khoảng tra cứu phải có từ trước đến");
        var keys = permissions.permissions(user);
        var allocation = new PermissionKey(Function.BED_ALLOCATION, Operation.VIEW);
        var infection = new PermissionKey(Function.INFECTION_CONTAINMENT, Operation.CREATE);
        List<ScopeFilter> filters = new ArrayList<>();
        for (var key : List.of(allocation, infection))
            if (keys.contains(key)) {
                var result = permissions.hasPermission(user, key, Target.UNSPECIFIED);
                if (result.allowed()
                        || result.failedLayer()
                                .filter(layer -> layer == FailedLayer.SCOPE)
                                .isPresent()) filters.add(scope.historyFilter(user, key, from, to));
            }
        if (filters.isEmpty())
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "Không có quyền tra cứu lịch sử vị trí");
        Set<Long> floors = new HashSet<>(), rooms = new HashSet<>(), residents = new HashSet<>();
        filters.forEach(value -> {
            floors.addAll(value.floorAreaIds());
            rooms.addAll(value.roomIds());
            residents.addAll(value.residentIds());
        });
        return new ScopeFilter(
                filters.stream().anyMatch(ScopeFilter::facilityWide),
                Set.copyOf(floors),
                Set.copyOf(rooms),
                Set.copyOf(residents));
    }

    private boolean visible(HistoryStay row, ScopeFilter filter) {
        return (filter.facilityWide()
                        || filter.floorAreaIds().contains(row.location().floorId())
                        || filter.roomIds().contains(row.location().roomId())
                        || filter.residentIds().contains(row.residentId()))
                && permitted(new Target(
                        "M03.BED_ALLOCATION",
                        Long.toString(row.allocationId()),
                        row.residentId(),
                        row.location().floorId()));
    }

    private boolean permitted(Target target) {
        return permissions
                        .hasPermission(user, new PermissionKey(Function.BED_ALLOCATION, Operation.VIEW), target)
                        .allowed()
                || permissions
                        .hasPermission(
                                user, new PermissionKey(Function.INFECTION_CONTAINMENT, Operation.CREATE), target)
                        .allowed();
    }
}

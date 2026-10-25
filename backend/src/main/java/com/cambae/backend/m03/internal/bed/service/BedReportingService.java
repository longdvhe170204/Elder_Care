package com.cambae.backend.m03.internal.bed.service;

import com.cambae.backend.common.error.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.*;
import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.api.spi.*;
import com.cambae.backend.m03.internal.bed.domain.BedAssetPort;
import com.cambae.backend.m03.internal.bed.domain.BedReportingSnapshot.*;
import com.cambae.backend.m03.internal.bed.persistence.BedReportingStore;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.parameter.api.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T061: API dữ liệu cho008/016; consumer kiểm quyền chức năng rồi truyền bộ lọc phạm vi. */
@Service
@Transactional(readOnly = true)
public class BedReportingService implements BedStatusApi, BedStatisticsApi, BedHandoverAlertApi {
    private final BedReportingStore store;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final ObjectProvider<AllocationInfectionStatus> infection;
    private final ObjectProvider<BedAssetPort> assets;

    public BedReportingService(
            BedReportingStore store,
            BusinessTime time,
            ParameterApi parameters,
            ObjectProvider<AllocationInfectionStatus> infection,
            ObjectProvider<BedAssetPort> assets) {
        this.store = store;
        this.time = time;
        this.parameters = parameters;
        this.infection = infection;
        this.assets = assets;
    }

    private void point(ScopeFilter scope, Instant at) {
        if (scope == null || at == null) throw new InvalidValueException("Cần phạm vi và thời điểm tra cứu");
    }

    private boolean visible(ScopeFilter scope, long floor, long room, Set<Long> residents) {
        return scope.facilityWide()
                || scope.floorAreaIds().contains(floor)
                || scope.roomIds().contains(room)
                || residents.stream().anyMatch(scope.residentIds()::contains);
    }

    private List<State> states(ScopeFilter scope, Instant at) {
        point(scope, at);
        var residents = store.residentsAt(at);
        return store.states(at).stream()
                .filter(s -> visible(scope, s.floorId(), s.roomId(), residents.getOrDefault(s.bedId(), Set.of())))
                .toList();
    }

    @Override
    public Map<BedStatus, Integer> countByState(ScopeFilter scope, Instant at) {
        Map<BedStatus, Integer> counts = new EnumMap<>(BedStatus.class);
        for (var s : BedStatus.values()) counts.put(s, 0);
        for (var row : states(scope, at)) counts.merge(row.state(), 1, Integer::sum);
        return Map.copyOf(counts);
    }

    @Override
    public Stream<BedStateOnDate> stateAt(ScopeFilter scope, Instant at) {
        return states(scope, at).stream().map(s -> new BedStateOnDate(s.bedId(), s.floorId(), s.state(), at));
    }

    @Override
    public int countAvailable(
            ScopeFilter scope, CareLevelCode level, ResidentGender gender, ResidencyType type, Instant at) {
        var empty = states(scope, at).stream()
                .filter(s -> s.state() == BedStatus.EMPTY)
                .map(State::bedId)
                .collect(Collectors.toSet());
        if (empty.isEmpty()) return 0;
        var source = infection.getIfAvailable();
        var asset = assets.getIfAvailable();
        if (source == null || asset == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SOURCE_UNAVAILABLE",
                    "Chưa đủ nguồn tính giường trống theo điều kiện");
        var cordon = source.cordonedAt(at);
        var flagged = asset.flaggedBeds(empty);
        return (int) store.policies().stream()
                .filter(p -> empty.contains(p.bedId())
                        && p.active()
                        && !p.isolated()
                        && !p.reserved()
                        && !flagged.contains(p.bedId())
                        && !cordon.floorIds().contains(p.floorId())
                        && !cordon.areaIds().contains(p.areaId()))
                .filter(p -> level == null || p.levels().contains(level))
                .filter(p -> gender == null
                        || p.gender() == GenderPolicy.UNRESTRICTED
                        || p.gender().name().equals(gender.name()))
                .filter(p -> type == null
                        || type == ResidencyType.LONG_TERM && p.longTerm()
                        || type == ResidencyType.SHORT_TERM && p.shortTerm())
                .count();
    }

    @Override
    public List<BedFlagView> flaggedBeds(ScopeFilter scope) {
        point(scope, time.now());
        List<BedFlagView> result = new ArrayList<>();
        for (var row : store.flags())
            if (!row.flags().isEmpty() && visible(scope, row.floorId(), row.roomId(), Set.of(row.residentId())))
                result.add(new BedFlagView(row.residentId(), row.bedId(), row.flags()));
        for (long resident : store.withoutBed())
            if (scope.facilityWide() || scope.residentIds().contains(resident))
                result.add(new BedFlagView(resident, null, Set.of("WITHOUT_BED")));
        return List.copyOf(result);
    }

    @Override
    public List<BedAlertView> byFloor(long floor, Instant at) {
        if (at == null) throw new InvalidValueException("Cần thời điểm tra cứu");
        return store.flags().stream()
                .filter(r -> r.floorId() == floor
                        && r.active()
                        && !r.from().isAfter(at)
                        && r.to().isAfter(at)
                        && r.flags().contains("AWAITING_DOCTOR_CONFIRMATION"))
                .map(r -> new BedAlertView(r.allocationId(), r.residentId(), r.bedId(), "AWAITING_DOCTOR_CONFIRMATION"))
                .toList();
    }

    @Override
    public Optional<Instant> emptyPeriodStarted(long bed) {
        if (store.current(bed).orElse(null) != BedStatus.EMPTY) return Optional.empty();
        var rows = store.history(bed, time.now());
        Instant start = null;
        for (int n = rows.size() - 1; n >= 0; n--) {
            var row = rows.get(n);
            if (row.state() == BedStatus.EMPTY) start = row.at();
            else if (row.state() != BedStatus.HELD || !row.event().equals("HOLD_WAITING_RECORD")) break;
        }
        return Optional.ofNullable(start);
    }

    private Instant deadline(State state) {
        return state.at()
                .plus(parameters.readAt(ParameterCode.CFG_M03_04, state.at()).asDuration());
    }

    @Override
    public List<BedView> overdueAwaitingReady(long floor, Instant at) {
        return states(new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()), at).stream()
                .filter(s -> s.state() == BedStatus.AWAITING_READY && at.isAfter(deadline(s)))
                .map(s -> new BedView(s.bedId(), s.roomId(), s.code(), s.state()))
                .toList();
    }

    @Override
    public Stream<AwaitingReadyInterval> awaitingReady(ScopeFilter scope, Instant from, Instant to) {
        point(scope, from);
        if (to == null || !from.isBefore(to)) throw new InvalidValueException("Khoảng tra cứu không hợp lệ");
        return store.windows(from, to).stream()
                .filter(w -> visible(
                        scope,
                        w.state().floorId(),
                        w.state().roomId(),
                        w.departedResidentId() == null ? Set.of() : Set.of(w.departedResidentId())))
                .map(w -> {
                    var start = w.state().at().isBefore(from) ? from : w.state().at();
                    var end = w.until() == null || w.until().isAfter(to) ? to : w.until();
                    return new AwaitingReadyInterval(
                            w.state().bedId(), w.state().floorId(), start, end, deadline(w.state()));
                })
                .filter(w -> w.from().isBefore(w.to()))
                .toList()
                .stream();
    }

    @Override
    public Stream<RoomIsolationView> isolatedRooms(ScopeFilter scope, Instant at) {
        point(scope, at);
        var residents = store.residentsAt(at);
        var policies = store.policies();
        return store.isolation(at).stream().filter(r -> {
            Set<Long> roomResidents = new HashSet<>();
            for (var p : policies)
                if (p.roomId() == r.roomId()) roomResidents.addAll(residents.getOrDefault(p.bedId(), Set.of()));
            return visible(scope, r.floorId(), r.roomId(), roomResidents);
        });
    }
}

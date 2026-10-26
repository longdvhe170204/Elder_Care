package com.cambae.backend.m03.internal.allocation.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.ResidentLocationApi;
import com.cambae.backend.m03.api.dto.LocationView;
import com.cambae.backend.m03.internal.allocation.domain.AllocationLocation;
import com.cambae.backend.m03.internal.allocation.domain.AllocationStatus;
import com.cambae.backend.m03.internal.allocation.persistence.AllocationStore;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.spi.BedAllocationApi;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.m15.account.api.spi.InvalidPermissionDataException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T026: bản thật nguồn vị trí cho M03/002, bất nhất phải báo lỗi nguồn FR-041b. */
@Service
@Transactional(readOnly = true)
public class AllocationQueries implements BedAllocationApi, ResidentLocationApi {
    private final AllocationStore store;
    private final BusinessTime time;

    public AllocationQueries(AllocationStore store, BusinessTime time) {
        this.store = store;
        this.time = time;
    }

    private List<AllocationLocation> checkedAt(Instant at) {
        var rows = store.at(at);
        Set<Long> residents = new HashSet<>();
        Set<Long> beds = new HashSet<>();
        for (var row : rows) {
            if (!residents.add(row.residentId()) || !beds.add(row.location().bedId()))
                throw new InvalidPermissionDataException(
                        "M03.BED_ALLOCATION", "CONFLICTING_ALLOCATION", row.allocationId());
        }
        return rows;
    }

    @Override
    public Optional<BedAllocationView> openAt(long residentId, Instant at) {
        return checkedAt(at).stream()
                .filter(row -> row.residentId() == residentId)
                .findFirst()
                .map(AllocationLocation::scopeView);
    }

    @Override
    public Set<Long> residentsIn(ScopeFilter scope, Instant at) {
        return checkedAt(at).stream()
                .filter(row -> scope.facilityWide()
                        || scope.floorAreaIds().contains(row.location().floorId())
                        || scope.roomIds().contains(row.location().roomId())
                        || scope.residentIds().contains(row.residentId()))
                .map(AllocationLocation::residentId)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<LocationView> current(long residentId) {
        return at(residentId, time.now());
    }

    @Override
    public Map<Long, LocationView> current(Collection<Long> residentIds) {
        if (residentIds.isEmpty()) return Map.of();
        Set<Long> ids = new HashSet<>(residentIds);
        Map<Long, LocationView> positions = new HashMap<>();
        checkedAt(time.now()).stream()
                .filter(row -> ids.contains(row.residentId()))
                .forEach(row -> positions.put(row.residentId(), row.location()));
        return Map.copyOf(positions);
    }

    @Override
    public Optional<LocationView> at(long residentId, Instant at) {
        return checkedAt(at).stream()
                .filter(row -> row.residentId() == residentId)
                .findFirst()
                .map(AllocationLocation::location);
    }

    @Override
    public Optional<Long> floorOn(long residentId, LocalDate date) {
        return at(residentId, time.startOfDay(date)).map(LocationView::floorId);
    }

    @Override
    public boolean hasNoBed(long residentId) {
        return store.hasNoBed(residentId);
    }

    @Override
    public Optional<BedAllocationView> currentOrFutureAllocation(long residentId) {
        // Kiểm nguồn hiện hành cả khi ưu tiên bản ACTIVE trước bản FUTURE.
        checkedAt(time.now());
        var rows = store.openOrFuture(residentId);
        for (var status : List.of(AllocationStatus.ACTIVE, AllocationStatus.FUTURE)) {
            if (rows.stream().filter(row -> row.status() == status).count() > 1)
                throw new InvalidPermissionDataException("M03.BED_ALLOCATION", "CONFLICTING_ALLOCATION", residentId);
        }
        return rows.stream().findFirst().map(AllocationLocation::scopeView);
    }
}

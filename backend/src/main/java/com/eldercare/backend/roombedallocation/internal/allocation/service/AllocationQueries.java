package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.spi.ContractApi;
import com.eldercare.backend.roombedallocation.api.LocationApi;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationLocation;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationStatus;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationStore;
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
public class AllocationQueries implements BedAllocationApi, ResidentLocationApi, LocationApi {
    private final AllocationStore store;
    private final BusinessTime time;
    private final LocationStructureApi locations;
    private final ContractApi contracts;

    public AllocationQueries(
            AllocationStore store, BusinessTime time, LocationStructureApi locations, ContractApi contracts) {
        this.store = store;
        this.time = time;
        this.locations = locations;
        this.contracts = contracts;
    }

    /** Q-403: không chọn tùy ý một tầng khi khu nghỉ nằm trong khu vực có nhiều tầng. */
    @Override
    public Optional<Long> dayCareFloorAt(long residentId, Instant at) {
        if (openAt(residentId, at).isPresent()
                || contracts
                        .currentResidenceType(residentId, LocalDate.ofInstant(at, BusinessTime.ZONE))
                        .filter(t -> t == ResidenceType.DAY_CARE)
                        .isEmpty()) return Optional.empty();
        var location = locations.dayCareRestAreaLocation().filter(locations::active);
        if (location.isEmpty()) return Optional.empty();
        var ref = location.get();
        if ("FLOOR".equals(ref.type())) return Optional.of(ref.id());
        if (!"AREA".equals(ref.type())) return Optional.empty();
        var floors = locations.floorsInArea(ref.id()).stream()
                .filter(id -> locations.active(new LocationRef("FLOOR", id)))
                .toList();
        if (floors.size() > 1)
            throw new InvalidPermissionDataException("M03.DAY_CARE_REST_AREA", "AMBIGUOUS_DAY_CARE_FLOOR", ref.id());
        return floors.stream().findFirst();
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

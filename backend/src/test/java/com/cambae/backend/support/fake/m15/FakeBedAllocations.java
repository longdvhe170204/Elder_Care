package com.cambae.backend.support.fake.m15;

import com.cambae.backend.m15.account.api.BedAllocationApi;
import com.cambae.backend.m15.account.api.BedAllocationView;
import com.cambae.backend.m15.account.api.ScopeFilter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Bản giả {@link BedAllocationApi} (003 cài): phân bổ test đặt sẵn, không xét thời điểm. */
public class FakeBedAllocations implements BedAllocationApi {

    private final Map<Long, BedAllocationView> allocations = new HashMap<>();

    public FakeBedAllocations allocate(long residentId, BedAllocationView allocation) {
        allocations.put(residentId, allocation);
        return this;
    }

    public FakeBedAllocations release(long residentId) {
        allocations.remove(residentId);
        return this;
    }

    @Override
    public Optional<BedAllocationView> openAt(long residentId, Instant at) {
        return Optional.ofNullable(allocations.get(residentId));
    }

    @Override
    public Set<Long> residentsIn(ScopeFilter scope, Instant at) {
        return allocations.entrySet().stream()
                .filter(e -> scope.facilityWide()
                        || scope.floorAreaIds().contains(e.getValue().floorAreaId())
                        || scope.roomIds().contains(e.getValue().roomId()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
